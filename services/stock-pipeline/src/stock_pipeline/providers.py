from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime
from typing import Any

from stock_pipeline.watchlist import WatchlistSymbol


@dataclass
class ProviderResult:
    daily_prices: list[dict[str, Any]]
    fundamentals: list[dict[str, Any]]
    news: list[dict[str, Any]]
    snapshots: list[dict[str, Any]]


def _coerce_datetime(value: Any) -> datetime | None:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value
    text = str(value).strip()
    if not text:
        return None
    for fmt in (
        "%Y-%m-%d %H:%M:%S",
        "%Y-%m-%d %H:%M",
        "%Y-%m-%d",
        "%Y/%m/%d %H:%M:%S",
        "%Y/%m/%d",
    ):
        try:
            return datetime.strptime(text, fmt)
        except ValueError:
            continue
    return None


def _coerce_date(value: Any) -> date | None:
    dt = _coerce_datetime(value)
    return dt.date() if dt else None


def _safe_float(value: Any) -> float | None:
    if value is None:
        return None
    text = str(value).replace(",", "").replace("%", "").strip()
    if text in {"", "nan", "None", "-"}:
        return None
    try:
        return float(text)
    except ValueError:
        return None


class MarketDataProvider:
    def fetch_bundle(
        self,
        symbol: WatchlistSymbol,
        start_date: date,
        end_date: date,
    ) -> ProviderResult:
        if symbol.market == "CN":
            return self._fetch_cn_bundle(symbol, start_date, end_date)
        return self._fetch_us_bundle(symbol, start_date, end_date)

    def fetch_cn_snapshots(self, symbols: list[WatchlistSymbol]) -> dict[str, dict[str, Any]]:
        results: dict[str, dict[str, Any]] = {}
        target_codes = {symbol.code for symbol in symbols if symbol.market == "CN"}
        if not target_codes:
            return results

        try:
            import akshare as ak

            spot_df = ak.stock_zh_a_spot_em()
            code_column = "代码" if "代码" in spot_df.columns else "symbol"
            filtered = spot_df[spot_df[code_column].astype(str).isin(target_codes)]
            for _, row in filtered.iterrows():
                code = str(row.get(code_column))
                results[code] = {
                    "price": _safe_float(row.get("最新价") or row.get("最新")),
                    "change_pct": _safe_float(row.get("涨跌幅")),
                    "volume": _safe_float(row.get("成交量")),
                    "amount": _safe_float(row.get("成交额")),
                    "bid": _safe_float(row.get("买一")),
                    "ask": _safe_float(row.get("卖一")),
                    "raw_payload": row.to_dict(),
                }
        except Exception:
            results = {}

        missing = [code for code in target_codes if code not in results]
        if missing:
            try:
                import efinance as ef

                for code in missing:
                    try:
                        snap = ef.stock.get_quote_snapshot(code)
                        if snap is None or snap.empty:
                            continue
                        payload = snap.to_dict()
                        results[code] = {
                            "price": _safe_float(payload.get("最新价") or payload.get("最新") or payload.get("最新价(元)") or payload.get("收盘")),
                            "change_pct": _safe_float(payload.get("涨跌幅")),
                            "volume": _safe_float(payload.get("成交量")),
                            "amount": _safe_float(payload.get("成交额")),
                            "bid": _safe_float(payload.get("买一")),
                            "ask": _safe_float(payload.get("卖一")),
                            "raw_payload": payload,
                        }
                    except Exception:
                        continue
            except Exception:
                pass

        return results

    def _fetch_cn_bundle(
        self,
        symbol: WatchlistSymbol,
        start_date: date,
        end_date: date,
    ) -> ProviderResult:
        import akshare as ak

        daily_prices: list[dict[str, Any]] = []
        try:
            hist_df = ak.stock_zh_a_hist(
                symbol=symbol.code,
                period="daily",
                start_date=start_date.strftime("%Y%m%d"),
                end_date=end_date.strftime("%Y%m%d"),
                adjust="qfq",
            )
            for _, row in hist_df.iterrows():
                daily_prices.append(
                    {
                        "trade_date": _coerce_date(row.get("日期")),
                        "open": _safe_float(row.get("开盘")),
                        "high": _safe_float(row.get("最高")),
                        "low": _safe_float(row.get("最低")),
                        "close": _safe_float(row.get("收盘")),
                        "adj_close": _safe_float(row.get("收盘")),
                        "volume": _safe_float(row.get("成交量")),
                        "turnover": _safe_float(row.get("成交额")),
                        "amplitude": _safe_float(row.get("振幅")),
                        "pct_change": _safe_float(row.get("涨跌幅")),
                        "change_amount": _safe_float(row.get("涨跌额")),
                        "source": "akshare",
                        "raw_payload": row.to_dict(),
                    }
                )
        except Exception:
            daily_prices = []

        if not daily_prices:
            try:
                import baostock as bs

                if symbol.exchange and symbol.exchange in {"SH", "SZ"}:
                    bs_code = f"{symbol.exchange.lower()}.{symbol.code}"
                else:
                    bs_code = None
                if bs_code:
                    lg = bs.login()
                    if lg.error_code == "0":
                        rs = bs.query_history_k_data_plus(
                            bs_code,
                            fields="date,open,high,low,close,volume,amount,turn,pctChg",
                            start_date=start_date.isoformat(),
                            end_date=end_date.isoformat(),
                            frequency="d",
                            adjustflag="2",
                        )
                        while rs.next():
                            row = rs.get_row_data()
                            daily_prices.append(
                                {
                                    "trade_date": _coerce_date(row[0]),
                                    "open": _safe_float(row[1]),
                                    "high": _safe_float(row[2]),
                                    "low": _safe_float(row[3]),
                                    "close": _safe_float(row[4]),
                                    "adj_close": _safe_float(row[4]),
                                    "volume": _safe_float(row[5]),
                                    "turnover": _safe_float(row[6]),
                                    "amplitude": None,
                                    "pct_change": _safe_float(row[8]),
                                    "change_amount": None,
                                    "source": "baostock",
                                    "raw_payload": {
                                        "date": row[0],
                                        "open": row[1],
                                        "high": row[2],
                                        "low": row[3],
                                        "close": row[4],
                                        "volume": row[5],
                                        "amount": row[6],
                                        "turn": row[7],
                                        "pctChg": row[8],
                                    },
                                }
                            )
                    bs.logout()
            except Exception:
                pass

        info_df = ak.stock_individual_info_em(symbol=symbol.code)
        info_map = {str(row["item"]).strip(): row["value"] for _, row in info_df.iterrows()}

        pe_ttm = _safe_float(info_map.get("市盈率-动态"))
        pb = _safe_float(info_map.get("市净率"))
        ps = _safe_float(info_map.get("市销率"))
        dividend_yield = _safe_float(info_map.get("股息率"))
        total_market_cap = _safe_float(info_map.get("总市值"))
        float_market_cap = _safe_float(info_map.get("流通市值"))

        # If key valuation fields are missing, enrich from 东方财富-估值历史 (AkShare stock_value_em)
        value_payload: dict[str, Any] | None = None
        try:
            if pe_ttm is None or pb is None or ps is None or dividend_yield is None:
                value_df = ak.stock_value_em(symbol=symbol.code)
                if not value_df.empty:
                    latest = value_df.tail(1).iloc[0].to_dict()
                    value_payload = latest
                    pe_ttm = pe_ttm if pe_ttm is not None else _safe_float(latest.get("PE(TTM)"))
                    pb = pb if pb is not None else _safe_float(latest.get("市净率"))
                    ps = ps if ps is not None else _safe_float(latest.get("市销率"))
                    # stock_value_em doesn't always contain dividend yield; keep existing if present
                    total_market_cap = total_market_cap if total_market_cap is not None else _safe_float(latest.get("总市值"))
                    float_market_cap = float_market_cap if float_market_cap is not None else _safe_float(latest.get("流通市值"))
        except Exception:
            value_payload = None

        fundamentals = [
            {
                "trade_date": end_date,
                "pe_ttm": pe_ttm,
                "pb": pb,
                "ps": ps,
                "dividend_yield": dividend_yield,
                "total_market_cap": total_market_cap,
                "float_market_cap": float_market_cap,
                "revenue": None,
                "net_profit": None,
                "roe": None,
                "debt_ratio": None,
                "source": "akshare",
                "raw_payload": {"individual_info": info_map, "value_em": value_payload},
            }
        ]

        news_rows: list[dict[str, Any]] = []
        try:
            news_df = ak.stock_news_em(symbol=symbol.code)
            for _, row in news_df.iterrows():
                news_rows.append(
                    {
                        "published_at": _coerce_datetime(row.get("发布时间") or row.get("时间")),
                        "title": str(row.get("新闻标题") or row.get("标题") or "").strip()[:512],
                        "url": str(row.get("新闻链接") or row.get("链接") or "")[:1024] or None,
                        "summary": str(row.get("新闻内容") or row.get("摘要") or "") or None,
                        "source": "akshare",
                        "raw_payload": row.to_dict(),
                    }
                )
        except Exception:
            pass

        return ProviderResult(
            daily_prices=[row for row in daily_prices if row["trade_date"] is not None],
            fundamentals=fundamentals,
            news=[row for row in news_rows if row["published_at"] and row["title"]],
            snapshots=[],
        )

    def _fetch_us_bundle(
        self,
        symbol: WatchlistSymbol,
        start_date: date,
        end_date: date,
    ) -> ProviderResult:
        import yfinance as yf

        ticker = yf.Ticker(symbol.code)
        from datetime import timedelta

        hist_df = ticker.history(
            start=start_date.isoformat(),
            end=(end_date + timedelta(days=1)).isoformat(),
            auto_adjust=False,
        )
        daily_prices: list[dict[str, Any]] = []
        if not hist_df.empty:
            for index, row in hist_df.iterrows():
                trade_date = index.date() if hasattr(index, "date") else _coerce_date(index)
                daily_prices.append(
                    {
                        "trade_date": trade_date,
                        "open": _safe_float(row.get("Open")),
                        "high": _safe_float(row.get("High")),
                        "low": _safe_float(row.get("Low")),
                        "close": _safe_float(row.get("Close")),
                        "adj_close": _safe_float(row.get("Adj Close") or row.get("Close")),
                        "volume": _safe_float(row.get("Volume")),
                        "turnover": None,
                        "amplitude": None,
                        "pct_change": None,
                        "change_amount": None,
                        "source": "yfinance",
                        "raw_payload": row.to_dict(),
                    }
                )

        info = {}
        try:
            info = ticker.info or {}
        except Exception:
            info = {}
        fundamentals = [
            {
                "trade_date": end_date,
                "pe_ttm": _safe_float(info.get("trailingPE")),
                "pb": _safe_float(info.get("priceToBook")),
                "ps": _safe_float(info.get("priceToSalesTrailing12Months")),
                "dividend_yield": _safe_float(info.get("dividendYield")),
                "total_market_cap": _safe_float(info.get("marketCap")),
                "float_market_cap": _safe_float(info.get("enterpriseValue")),
                "revenue": _safe_float(info.get("totalRevenue")),
                "net_profit": _safe_float(info.get("netIncomeToCommon")),
                "roe": _safe_float(info.get("returnOnEquity")),
                "debt_ratio": _safe_float(info.get("debtToEquity")),
                "source": "yfinance",
                "raw_payload": info,
            }
        ]

        news_rows: list[dict[str, Any]] = []
        try:
            for item in ticker.news or []:
                news_rows.append(
                    {
                        "published_at": datetime.fromtimestamp(item.get("providerPublishTime")),
                        "title": str(item.get("title") or "").strip()[:512],
                        "url": str(item.get("link") or "")[:1024] or None,
                        "summary": str(item.get("summary") or "") or None,
                        "source": "yfinance",
                        "raw_payload": item,
                    }
                )
        except Exception:
            pass

        snapshot = []
        try:
            latest_df = ticker.history(period="1d", interval="1m")
            if not latest_df.empty:
                latest = latest_df.iloc[-1]
                index = latest_df.index[-1]
                snapshot.append(
                    {
                        "snapshot_time": index.to_pydatetime().replace(tzinfo=None),
                        "price": _safe_float(latest.get("Close")),
                        "change_pct": None,
                        "volume": _safe_float(latest.get("Volume")),
                        "amount": None,
                        "bid": None,
                        "ask": None,
                        "source": "yfinance",
                        "raw_payload": latest.to_dict(),
                    }
                )
        except Exception:
            pass

        return ProviderResult(
            daily_prices=[row for row in daily_prices if row["trade_date"] is not None],
            fundamentals=fundamentals,
            news=[row for row in news_rows if row["published_at"] and row["title"]],
            snapshots=snapshot,
        )
