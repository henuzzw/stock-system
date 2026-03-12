from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from typing import Any

import numpy as np
import pandas as pd


@dataclass(frozen=True)
class CandidateRow:
    run_date: date
    symbol_id: int
    in_technical_top20: int
    in_valuation_top20: int
    left_signal_score: float
    right_signal_score: float
    left_triggered: int
    right_triggered: int
    snapshot: dict[str, Any]


def _clamp(v: float, lo: float = 0.0, hi: float = 100.0) -> float:
    return float(min(max(v, lo), hi))


def build_candidates(
    *,
    run_date: date,
    technical_top: pd.DataFrame,
    valuation_top: pd.DataFrame,
    technical_components: pd.DataFrame,
    valuation_components: pd.DataFrame,
) -> list[dict[str, Any]]:
    """Return MySQL rows for candidates_daily.

    technical_top: columns [symbol_id]
    valuation_top: columns [symbol_id]
    technical_components: per symbol latest components incl close,sma20,sma60,price_percentile_120,drawdown_120,rsi_14(optional)
    valuation_components: per symbol latest components incl pe_ttm,pb,ps,dividend_yield(optional)
    """

    tech_set = set(technical_top["symbol_id"].astype(int).tolist()) if not technical_top.empty else set()
    val_set = set(valuation_top["symbol_id"].astype(int).tolist()) if not valuation_top.empty else set()
    universe = sorted(tech_set | val_set)
    if not universe:
        return []

    tech_map = technical_components.set_index("symbol_id").to_dict(orient="index") if not technical_components.empty else {}
    val_map = valuation_components.set_index("symbol_id").to_dict(orient="index") if not valuation_components.empty else {}

    rows: list[dict[str, Any]] = []
    for sid in universe:
        t = tech_map.get(sid, {})
        v = val_map.get(sid, {})

        # Left-side (catching a falling knife / deep low):
        # - price_percentile_120 < 20
        # - drawdown_120 > 20
        # - rsi_14 < 30 (optional)
        price_pct = t.get("price_percentile_120")
        drawdown = t.get("drawdown_120")
        rsi = t.get("rsi_14")

        left_hits = 0
        left_score = 0.0
        if price_pct is not None and not pd.isna(price_pct):
            # lower percentile => higher score
            left_score += _clamp(100.0 - float(price_pct)) * 0.45
            if float(price_pct) < 20:
                left_hits += 1
        if drawdown is not None and not pd.isna(drawdown):
            left_score += _clamp(float(drawdown)) * 0.40
            if float(drawdown) > 20:
                left_hits += 1
        if rsi is not None and not pd.isna(rsi):
            # lower RSI => higher score
            left_score += _clamp(100.0 - float(rsi)) * 0.15
            if float(rsi) < 30:
                left_hits += 1

        left_triggered = 1 if left_hits >= 2 else 0
        left_score = _clamp(left_score)

        # Right-side (safer: stabilization / turn-up):
        # - close > sma20
        # - sma20_today >= sma20_yesterday (we approximate via provided flag if available)
        # - not making new lows in last 10 days (flag if available)
        close = t.get("close")
        sma20 = t.get("sma20")
        sma20_up = t.get("sma20_up")
        no_new_low_10 = t.get("no_new_low_10")

        right_hits = 0
        right_score = 0.0
        if close is not None and sma20 is not None and not pd.isna(close) and not pd.isna(sma20) and float(sma20) != 0.0:
            above = 1.0 if float(close) > float(sma20) else 0.0
            right_score += above * 40.0
            if above > 0:
                right_hits += 1
        if sma20_up is not None and int(sma20_up) == 1:
            right_score += 30.0
            right_hits += 1
        if no_new_low_10 is not None and int(no_new_low_10) == 1:
            right_score += 30.0
            right_hits += 1

        right_triggered = 1 if right_hits >= 2 else 0
        right_score = _clamp(right_score)

        snapshot = {
            "close": close,
            "sma20": sma20,
            "sma60": t.get("sma60"),
            "price_percentile_120": price_pct,
            "drawdown_120": drawdown,
            "rsi_14": rsi,
            "pe_ttm": v.get("pe_ttm"),
            "pb": v.get("pb"),
            "ps": v.get("ps"),
            "dividend_yield": v.get("dividend_yield"),
        }

        rows.append(
            {
                "run_date": run_date,
                "symbol_id": int(sid),
                "in_technical_top20": 1 if sid in tech_set else 0,
                "in_valuation_top20": 1 if sid in val_set else 0,
                "left_signal_score": float(left_score),
                "right_signal_score": float(right_score),
                "left_triggered": int(left_triggered),
                "right_triggered": int(right_triggered),
                "rank_left": None,
                "rank_right": None,
                "snapshot": snapshot,
            }
        )

    return rows
