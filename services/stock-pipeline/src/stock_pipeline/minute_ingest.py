from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime, timedelta
from typing import Any

from stock_pipeline.db import Database
from stock_pipeline.watchlist import WatchlistSymbol


@dataclass
class MinuteBar:
    ts: datetime
    open: float | None
    high: float | None
    low: float | None
    close: float | None
    volume: float | None
    amount: float | None
    avg_price: float | None
    source: str
    raw_payload: dict[str, Any]


def _safe_float(value: Any) -> float | None:
    if value is None:
        return None
    text = str(value).replace(",", "").strip()
    if text in {"", "nan", "None", "-"}:
        return None
    try:
        return float(text)
    except ValueError:
        return None


def fetch_minute_bars_akshare(symbol: WatchlistSymbol, day: date) -> list[MinuteBar]:
    import akshare as ak

    start = f"{day.isoformat()} 09:15:00"
    end = f"{day.isoformat()} 15:00:00"
    df = ak.stock_zh_a_hist_min_em(symbol=symbol.code, start_date=start, end_date=end, period="1", adjust="")
    bars: list[MinuteBar] = []
    if df is None or df.empty:
        return bars

    for _, row in df.iterrows():
        ts = row.get("时间")
        ts_dt = ts if isinstance(ts, datetime) else datetime.fromisoformat(str(ts))
        payload = row.to_dict()
        bars.append(
            MinuteBar(
                ts=ts_dt,
                open=_safe_float(row.get("开盘")),
                high=_safe_float(row.get("最高")),
                low=_safe_float(row.get("最低")),
                close=_safe_float(row.get("收盘")),
                volume=_safe_float(row.get("成交量")),
                amount=_safe_float(row.get("成交额")),
                avg_price=_safe_float(row.get("均价")),
                source="akshare",
                raw_payload=payload,
            )
        )
    return bars


def upsert_minute_prices(db: Database, symbol_id: int, bars: list[MinuteBar]) -> int:
    rows = []
    for bar in bars:
        rows.append(
            {
                "symbol_id": symbol_id,
                "ts": bar.ts,
                "open": bar.open,
                "high": bar.high,
                "low": bar.low,
                "close": bar.close,
                "volume": int(bar.volume) if bar.volume is not None else None,
                "amount": bar.amount,
                "avg_price": bar.avg_price,
                "source": bar.source,
                "raw_payload": bar.raw_payload,
            }
        )
    return db.upsert_rows("minute_prices", rows, key_columns=["symbol_id", "ts"])
