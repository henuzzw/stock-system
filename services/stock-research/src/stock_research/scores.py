from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from typing import Any

import numpy as np
import pandas as pd


def _clamp(v: float, lo: float = 0.0, hi: float = 100.0) -> float:
    return float(min(max(v, lo), hi))


def _zscore(series: pd.Series) -> pd.Series:
    s = pd.to_numeric(series, errors="coerce").astype(float)
    mu = s.mean(skipna=True)
    sd = s.std(skipna=True)
    if sd == 0 or np.isnan(sd):
        return pd.Series(np.nan, index=series.index)
    return (s - mu) / sd


@dataclass(frozen=True)
class ScoresResult:
    run_date: date
    rows: list[dict[str, Any]]


def build_scores(
    *,
    run_date: date,
    daily_prices: pd.DataFrame,
    technicals: pd.DataFrame,
) -> ScoresResult:
    """Compute trend_ok + trend/risk/momentum scores using only stored daily_prices & technicals."""

    dp = daily_prices.copy()
    dp["trade_date"] = pd.to_datetime(dp["trade_date"])  # type: ignore
    for col in ["open", "high", "low", "close", "volume", "turnover"]:
        if col in dp.columns:
            dp[col] = pd.to_numeric(dp[col], errors="coerce")

    tech = technicals.copy()
    tech["trade_date"] = pd.to_datetime(tech["trade_date"])  # type: ignore
    for col in ["sma_20", "sma_60", "rsi_14"]:
        if col in tech.columns:
            tech[col] = pd.to_numeric(tech[col], errors="coerce")

    # join latest day per symbol
    latest = dp.sort_values(["symbol_id", "trade_date"]).groupby("symbol_id").tail(260)

    # merge same-day technicals
    merged = latest.merge(tech[["symbol_id", "trade_date", "sma_20", "rsi_14"]], on=["symbol_id", "trade_date"], how="left")

    rows: list[dict[str, Any]] = []

    for sid, g in merged.groupby("symbol_id", sort=False):
        g = g.sort_values("trade_date")
        cur = g.tail(1).iloc[0]

        close = float(cur["close"]) if not pd.isna(cur.get("close")) else None
        low = float(cur["low"]) if not pd.isna(cur.get("low")) else None
        sma20 = float(cur["sma_20"]) if not pd.isna(cur.get("sma_20")) else None
        sma60 = None

        # === Trend gate: satisfy >=2 of 3 ===
        conds = {}
        # 1) Close > SMA20
        conds["close_gt_sma20"] = bool(close is not None and sma20 is not None and close > sma20)

        # 2) SMA20 today >= SMA20 yesterday
        prev = g.tail(2).head(1) if len(g) >= 2 else None
        sma20_prev = None
        if prev is not None and not prev.empty and not pd.isna(prev.iloc[0].get("sma_20")):
            sma20_prev = float(prev.iloc[0]["sma_20"])
        conds["sma20_non_down"] = bool(sma20 is not None and sma20_prev is not None and sma20 >= sma20_prev)

        # 3) no new low in last 10 trading days (today low > min(low[-10]))
        last10 = g.tail(10)
        low10 = pd.to_numeric(last10["low"], errors="coerce")
        min_low10 = float(low10.min()) if not low10.dropna().empty else None
        conds["no_new_low_10"] = bool(low is not None and min_low10 is not None and low > min_low10)

        trend_hits = sum(1 for v in conds.values() if v)
        trend_ok = 1 if trend_hits >= 2 else 0

        # === Momentum score: 20d return (z-scored within universe) ===
        closes = pd.to_numeric(g["close"], errors="coerce")
        ret20 = None
        if closes.dropna().shape[0] >= 21:
            ret20 = float(closes.iloc[-1] / closes.iloc[-21] - 1.0)

        # === Risk score: 20d volatility (lower is better -> higher score) + max drawdown 60d (lower better) ===
        vol20 = None
        if closes.dropna().shape[0] >= 21:
            r = closes.pct_change().tail(20)
            if not r.dropna().empty:
                vol20 = float(r.std())

        dd60 = None
        if closes.dropna().shape[0] >= 61:
            window = closes.tail(60)
            peak = window.cummax()
            dd = (window / peak - 1.0).min()
            dd60 = float(dd)  # negative

        rows.append(
            {
                "symbol_id": int(sid),
                "run_date": run_date,
                "trend_ok": int(trend_ok),
                "_ret20": ret20,
                "_vol20": vol20,
                "_dd60": dd60,
                "reasons": {
                    "trend": {**conds, "hits": trend_hits},
                    "ret20": ret20,
                    "vol20": vol20,
                    "dd60": dd60,
                    "close": close,
                    "sma20": sma20,
                    "sma60": sma60,
                },
            }
        )

    df = pd.DataFrame(rows)
    if df.empty:
        return ScoresResult(run_date=run_date, rows=[])

    # Normalize to 0-100
    mom = _zscore(df["_ret20"]).clip(-3, 3)
    df["momentum_score"] = ((mom + 3) / 6 * 100).fillna(0).clip(0, 100)

    vol = _zscore(df["_vol20"]).clip(-3, 3)
    # higher vol => lower score
    df["risk_score"] = (100 - ((vol + 3) / 6 * 100)).fillna(50).clip(0, 100)

    # Trend score from hits + SMA alignment
    df["trend_score"] = (df["trend_ok"] * 70 + (df["reasons"].apply(lambda r: r["trend"]["hits"]) / 3 * 30)).astype(float)

    # Total score
    df["total_score"] = (df["trend_score"] * 0.45 + df["risk_score"] * 0.25 + df["momentum_score"] * 0.30).astype(float)

    out_rows: list[dict[str, Any]] = []
    for rec in df.to_dict(orient="records"):
        out_rows.append(
            {
                "run_date": rec["run_date"],
                "symbol_id": int(rec["symbol_id"]),
                "trend_ok": int(rec["trend_ok"]),
                "trend_score": float(rec["trend_score"]),
                "risk_score": float(rec["risk_score"]),
                "momentum_score": float(rec["momentum_score"]),
                "total_score": float(rec["total_score"]),
                "reasons": rec["reasons"],
            }
        )

    return ScoresResult(run_date=run_date, rows=out_rows)
