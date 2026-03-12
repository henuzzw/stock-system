from __future__ import annotations

from dataclasses import asdict
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Any

from stock_pipeline.config import Settings
from stock_pipeline.db import Database
from stock_pipeline.providers import MarketDataProvider
from stock_pipeline.watchlist import WatchlistSymbol, load_watchlist


class DataIngestor:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self.db = Database(settings)
        self.provider = MarketDataProvider()

    def init_database(self, migration_path: Path) -> None:
        self.db.execute_script(migration_path.read_text(encoding="utf-8"), include_database=False)

    def import_watchlist(self) -> int:
        symbols = load_watchlist(self.settings.watchlist_path)
        rows = [{**asdict(symbol), "is_active": 1} for symbol in symbols]
        return self.db.upsert_rows(
            "symbols",
            rows,
            key_columns=["code", "market"],
            update_columns=["name", "exchange", "source", "is_active"],
        )

    def get_symbols(self, codes: list[str] | None = None) -> list[dict[str, Any]]:
        if codes:
            placeholders = ", ".join(["%s"] * len(codes))
            normalized = [code.upper() if not code.isdigit() else code.zfill(6) for code in codes]
            return self.db.fetchall(
                f"SELECT * FROM symbols WHERE code IN ({placeholders}) AND is_active = 1 ORDER BY market, code",
                tuple(normalized),
            )
        return self.db.fetchall("SELECT * FROM symbols WHERE is_active = 1 ORDER BY market, code")

    def ingest_market_data(
        self,
        codes: list[str] | None = None,
        start_date: date | None = None,
        end_date: date | None = None,
        include_news: bool = True,
        include_snapshots: bool = True,
    ) -> int:
        start_date = start_date or (date.today() - timedelta(days=self.settings.default_lookback_days))
        end_date = end_date or date.today()
        total_rows = 0

        symbols = self.get_symbols(codes)
        watchlist_symbols = [
            WatchlistSymbol(
                code=row["code"],
                name=row["name"],
                market=row["market"],
                exchange=row["exchange"],
                source=row["source"],
            )
            for row in symbols
        ]
        cn_snapshot_map = self.provider.fetch_cn_snapshots(watchlist_symbols) if include_snapshots else {}

        from tqdm import tqdm

        for row, symbol in tqdm(list(zip(symbols, watchlist_symbols)), total=len(watchlist_symbols)):
            started_at = datetime.now()
            rows_written = 0
            source = "akshare" if symbol.market == "CN" else "yfinance"
            try:
                bundle = self.provider.fetch_bundle(symbol, start_date, end_date)
                snapshot_rows = bundle.snapshots
                if symbol.market == "CN" and symbol.code in cn_snapshot_map:
                    snapshot_rows = [
                        {
                            "snapshot_time": datetime.now().replace(second=0, microsecond=0),
                            "source": "akshare",
                            **cn_snapshot_map[symbol.code],
                        }
                    ]

                with self.db.session():
                    rows_written += self._upsert_daily_prices(row["id"], bundle.daily_prices)
                    rows_written += self._upsert_fundamentals(row["id"], bundle.fundamentals)
                    rows_written += self._upsert_technicals(row["id"])
                    if include_news:
                        rows_written += self._upsert_news(row["id"], bundle.news)
                    if include_snapshots:
                        rows_written += self._upsert_snapshots(row["id"], snapshot_rows)
                    self.db.insert_ingestion_log(
                        run_type="market_data",
                        symbol_id=row["id"],
                        source=source,
                        status="success",
                        started_at=started_at,
                        finished_at=datetime.now(),
                        rows_written=rows_written,
                        message=None,
                    )
                total_rows += rows_written
            except Exception as exc:
                self.db.insert_ingestion_log(
                    run_type="market_data",
                    symbol_id=row["id"],
                    source=source,
                    status="failed",
                    started_at=started_at,
                    finished_at=datetime.now(),
                    rows_written=rows_written,
                    message=str(exc)[:1000],
                )
        return total_rows

    def intraday_update(self, codes: list[str] | None = None) -> int:
        return self.ingest_market_data(
            codes=codes,
            start_date=date.today() - timedelta(days=10),
            end_date=date.today(),
            include_news=False,
            include_snapshots=True,
        )

    def daily_close_recap(self, codes: list[str] | None = None) -> int:
        return self.ingest_market_data(
            codes=codes,
            start_date=date.today() - timedelta(days=max(30, self.settings.default_lookback_days)),
            end_date=date.today(),
            include_news=True,
            include_snapshots=True,
        )

    def _upsert_daily_prices(self, symbol_id: int, rows: list[dict[str, Any]]) -> int:
        prepared = [{"symbol_id": symbol_id, **row} for row in rows if row.get("trade_date")]
        return self.db.upsert_rows("daily_prices", prepared, key_columns=["symbol_id", "trade_date"])

    def _upsert_fundamentals(self, symbol_id: int, rows: list[dict[str, Any]]) -> int:
        prepared = [{"symbol_id": symbol_id, **row} for row in rows if row.get("trade_date")]
        return self.db.upsert_rows("fundamentals", prepared, key_columns=["symbol_id", "trade_date"])

    def _upsert_news(self, symbol_id: int, rows: list[dict[str, Any]]) -> int:
        prepared = [{"symbol_id": symbol_id, **row} for row in rows if row.get("published_at") and row.get("title")]
        if not prepared:
            return 0
        return self.db.upsert_rows(
            "news",
            prepared,
            key_columns=["symbol_id", "published_at", "title"],
            update_columns=["url", "summary", "source", "raw_payload"],
        )

    def _upsert_snapshots(self, symbol_id: int, rows: list[dict[str, Any]]) -> int:
        prepared = [{"symbol_id": symbol_id, **row} for row in rows if row.get("snapshot_time")]
        if not prepared:
            return 0
        return self.db.upsert_rows("snapshots", prepared, key_columns=["symbol_id", "snapshot_time"])

    def _upsert_technicals(self, symbol_id: int) -> int:
        import pandas as pd

        rows = self.db.fetchall(
            """
            SELECT trade_date, close
            FROM daily_prices
            WHERE symbol_id = %s
            ORDER BY trade_date
            """,
            (symbol_id,),
        )
        if not rows:
            return 0
        df = pd.DataFrame(rows)
        df["close"] = pd.to_numeric(df["close"], errors="coerce")
        df["sma_5"] = df["close"].rolling(5).mean()
        df["sma_10"] = df["close"].rolling(10).mean()
        df["sma_20"] = df["close"].rolling(20).mean()
        df["ema_12"] = df["close"].ewm(span=12, adjust=False).mean()
        df["ema_26"] = df["close"].ewm(span=26, adjust=False).mean()
        delta = df["close"].diff()
        gain = delta.clip(lower=0).rolling(14).mean()
        loss = (-delta.clip(upper=0)).rolling(14).mean()
        rs = gain / loss
        df["rsi_14"] = 100 - (100 / (1 + rs))
        df["macd"] = df["ema_12"] - df["ema_26"]
        df["macd_signal"] = df["macd"].ewm(span=9, adjust=False).mean()
        df["macd_hist"] = df["macd"] - df["macd_signal"]

        technical_rows = []
        for _, row in df.iterrows():
            technical_rows.append(
                {
                    "symbol_id": symbol_id,
                    "trade_date": row["trade_date"],
                    "sma_5": row["sma_5"],
                    "sma_10": row["sma_10"],
                    "sma_20": row["sma_20"],
                    "ema_12": row["ema_12"],
                    "ema_26": row["ema_26"],
                    "rsi_14": row["rsi_14"],
                    "macd": row["macd"],
                    "macd_signal": row["macd_signal"],
                    "macd_hist": row["macd_hist"],
                    "source": "derived",
                    "raw_payload": {"close": row["close"]},
                }
            )
        return self.db.upsert_rows("technicals", technical_rows, key_columns=["symbol_id", "trade_date"])
