from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from typing import Any

import numpy as np
import pandas as pd


def _safe_float(value: Any) -> float | None:
    if value is None:
        return None
    try:
        result = float(value)
    except (TypeError, ValueError):
        return None
    if np.isnan(result):
        return None
    return result


def _clamp(value: float, lower: float = 0.0, upper: float = 100.0) -> float:
    return float(min(max(value, lower), upper))


def _normalize_series(
    series: pd.Series,
    *,
    invert: bool = False,
    clip_lower: float | None = None,
    clip_upper: float | None = None,
) -> pd.Series:
    numeric = pd.to_numeric(series, errors="coerce").astype(float)
    if clip_lower is not None or clip_upper is not None:
        numeric = numeric.clip(lower=clip_lower, upper=clip_upper)

    valid = numeric.dropna()
    if valid.empty:
        return pd.Series(np.nan, index=series.index, dtype=float)

    min_value = float(valid.min())
    max_value = float(valid.max())
    if np.isclose(min_value, max_value):
        normalized = pd.Series(50.0, index=series.index, dtype=float)
        normalized[series.isna()] = np.nan
        return normalized

    scaled = (numeric - min_value) / (max_value - min_value) * 100.0
    if invert:
        scaled = 100.0 - scaled
    return scaled


@dataclass(frozen=True)
class RankingResult:
    run_date: date
    table: str
    rows: list[dict[str, Any]]
    top20: list[dict[str, Any]]


class TechnicalLowRanker:
    table_name = "technical_low_daily"

    def build_rankings(self, source_rows: list[dict[str, Any]], run_date: date | None = None) -> RankingResult:
        frame = pd.DataFrame(source_rows)
        if frame.empty:
            run_date = run_date or date.today()
            return RankingResult(run_date=run_date, table=self.table_name, rows=[], top20=[])

        frame["trade_date"] = pd.to_datetime(frame["trade_date"])
        frame["close"] = pd.to_numeric(frame["close"], errors="coerce")
        frame["sma_20"] = pd.to_numeric(frame["sma_20"], errors="coerce")
        frame = frame.sort_values(["symbol_id", "trade_date"]).copy()

        ranked_rows: list[dict[str, Any]] = []
        for symbol_id, group in frame.groupby("symbol_id", sort=False):
            recent = group.tail(120).copy()
            current = recent.iloc[-1]

            closes = recent["close"].dropna()
            if closes.empty:
                continue

            latest_close = float(closes.iloc[-1])
            window_low = float(closes.min())
            window_high = float(closes.max())
            window_range = window_high - window_low
            if np.isclose(window_range, 0.0):
                price_percentile_120 = 50.0
            else:
                price_percentile_120 = ((latest_close - window_low) / window_range) * 100.0

            peak_120 = window_high
            drawdown_120 = 0.0 if np.isclose(peak_120, 0.0) else ((peak_120 - latest_close) / peak_120) * 100.0

            sma20 = _safe_float(current["sma_20"])
            distance_to_sma20 = None if sma20 in (None, 0.0) else ((latest_close - sma20) / sma20) * 100.0

            sma60_series = recent["close"].tail(60)
            sma60 = None if len(sma60_series.dropna()) < 60 else float(sma60_series.mean())
            distance_to_sma60 = None if sma60 in (None, 0.0) else ((latest_close - sma60) / sma60) * 100.0

            ranked_rows.append(
                {
                    "symbol_id": int(symbol_id),
                    "code": current["code"],
                    "name": current["name"],
                    "trade_date": current["trade_date"].date(),
                    "price_percentile_120": _clamp(price_percentile_120),
                    "drawdown_120": _clamp(drawdown_120),
                    "distance_to_sma20": distance_to_sma20,
                    "distance_to_sma60": distance_to_sma60,
                    "sma20_available": sma20 is not None,
                    "sma60_available": sma60 is not None,
                    "history_days": int(len(closes)),
                }
            )

        ranked = pd.DataFrame(ranked_rows)
        if ranked.empty:
            resolved_run_date = run_date or date.today()
            return RankingResult(run_date=resolved_run_date, table=self.table_name, rows=[], top20=[])

        percentile_component = 100.0 - ranked["price_percentile_120"]
        drawdown_component = ranked["drawdown_120"]
        sma20_component = _normalize_series(ranked["distance_to_sma20"], invert=True, clip_lower=-30.0, clip_upper=30.0)
        sma60_component = _normalize_series(ranked["distance_to_sma60"], invert=True, clip_lower=-30.0, clip_upper=30.0)

        weights = {"percentile": 0.35, "drawdown": 0.30, "sma20": 0.20, "sma60": 0.15}
        component_frame = pd.DataFrame(
            {
                "percentile": percentile_component,
                "drawdown": drawdown_component,
                "sma20": sma20_component,
                "sma60": sma60_component,
            }
        )
        available_weights = component_frame.notna().mul(pd.Series(weights))
        weighted_score = component_frame.fillna(0.0).mul(pd.Series(weights)).sum(axis=1)
        total_weight = available_weights.sum(axis=1)
        ranked["score"] = (weighted_score / total_weight.replace(0.0, np.nan)).fillna(0.0).clip(0.0, 100.0)
        ranked["component_score_percentile"] = component_frame["percentile"]
        ranked["component_score_drawdown"] = component_frame["drawdown"]
        ranked["component_score_sma20"] = component_frame["sma20"]
        ranked["component_score_sma60"] = component_frame["sma60"]
        ranked = ranked.sort_values(["score", "symbol_id"], ascending=[False, True]).reset_index(drop=True)
        ranked["rank"] = np.arange(1, len(ranked) + 1)

        resolved_run_date = run_date or max(row["trade_date"] for row in ranked_rows)
        output_rows: list[dict[str, Any]] = []
        for row in ranked.to_dict(orient="records"):
            reasons = {
                "latest_trade_date": row["trade_date"].isoformat(),
                "price_percentile_120": round(float(row["price_percentile_120"]), 4),
                "drawdown_120": round(float(row["drawdown_120"]), 4),
                "distance_to_sma20": None if pd.isna(row["distance_to_sma20"]) else round(float(row["distance_to_sma20"]), 4),
                "distance_to_sma60": None if pd.isna(row["distance_to_sma60"]) else round(float(row["distance_to_sma60"]), 4),
                "component_scores": {
                    "price_percentile_120": round(float(row["component_score_percentile"]), 4),
                    "drawdown_120": round(float(row["component_score_drawdown"]), 4),
                    "distance_to_sma20": None if pd.isna(row["component_score_sma20"]) else round(float(row["component_score_sma20"]), 4),
                    "distance_to_sma60": None if pd.isna(row["component_score_sma60"]) else round(float(row["component_score_sma60"]), 4),
                },
                "history_days": int(row["history_days"]),
                "missing": [
                    name
                    for name, available in (
                        ("sma20", row["sma20_available"]),
                        ("sma60", row["sma60_available"]),
                    )
                    if not available
                ],
            }
            output_rows.append(
                {
                    "run_date": resolved_run_date,
                    "symbol_id": int(row["symbol_id"]),
                    "score": round(float(row["score"]), 4),
                    "rank": int(row["rank"]),
                    "reasons": reasons,
                }
            )

        return RankingResult(
            run_date=resolved_run_date,
            table=self.table_name,
            rows=output_rows,
            top20=ranked.head(20).to_dict(orient="records"),
        )


class ValuationLowRanker:
    table_name = "valuation_low_daily"

    def build_rankings(self, source_rows: list[dict[str, Any]], run_date: date | None = None) -> RankingResult:
        frame = pd.DataFrame(source_rows)
        if frame.empty:
            run_date = run_date or date.today()
            return RankingResult(run_date=run_date, table=self.table_name, rows=[], top20=[])

        frame["trade_date"] = pd.to_datetime(frame["trade_date"])
        for column in ["pe_ttm", "pb", "ps", "dividend_yield"]:
            frame[column] = pd.to_numeric(frame[column], errors="coerce")

        latest_trade_date = frame.groupby("symbol_id")["trade_date"].transform("max")
        latest = frame.loc[frame["trade_date"] == latest_trade_date].copy()
        latest = latest.sort_values(["symbol_id"]).reset_index(drop=True)

        latest["pe_ttm_clean"] = latest["pe_ttm"].where(latest["pe_ttm"] > 0)
        latest["pb_clean"] = latest["pb"].where(latest["pb"] > 0)
        latest["ps_clean"] = latest["ps"].where(latest["ps"] > 0)
        latest["dividend_yield_clean"] = latest["dividend_yield"].where(latest["dividend_yield"] >= 0)

        component_frame = pd.DataFrame(
            {
                "pe_ttm": _normalize_series(latest["pe_ttm_clean"], invert=True),
                "pb": _normalize_series(latest["pb_clean"], invert=True),
                "ps": _normalize_series(latest["ps_clean"], invert=True),
                "dividend_yield": _normalize_series(latest["dividend_yield_clean"], invert=False),
            }
        )
        weights = {"pe_ttm": 0.35, "pb": 0.30, "ps": 0.20, "dividend_yield": 0.15}
        available_weights = component_frame.notna().mul(pd.Series(weights))
        weighted_score = component_frame.fillna(0.0).mul(pd.Series(weights)).sum(axis=1)
        total_weight = available_weights.sum(axis=1)
        latest["score"] = (weighted_score / total_weight.replace(0.0, np.nan)).fillna(0.0).clip(0.0, 100.0)
        latest["component_score_pe_ttm"] = component_frame["pe_ttm"]
        latest["component_score_pb"] = component_frame["pb"]
        latest["component_score_ps"] = component_frame["ps"]
        latest["component_score_dividend_yield"] = component_frame["dividend_yield"]

        latest = latest.sort_values(["score", "symbol_id"], ascending=[False, True]).reset_index(drop=True)
        latest["rank"] = np.arange(1, len(latest) + 1)
        resolved_run_date = run_date or latest["trade_date"].max().date()

        output_rows: list[dict[str, Any]] = []
        for row in latest.to_dict(orient="records"):
            missing = [
                field
                for field in ["pe_ttm", "pb", "ps", "dividend_yield"]
                if pd.isna(row[f"{field}_clean"])
            ]
            reasons = {
                "latest_trade_date": row["trade_date"].date().isoformat(),
                "metrics": {
                    "pe_ttm": None if pd.isna(row["pe_ttm"]) else round(float(row["pe_ttm"]), 4),
                    "pb": None if pd.isna(row["pb"]) else round(float(row["pb"]), 4),
                    "ps": None if pd.isna(row["ps"]) else round(float(row["ps"]), 4),
                    "dividend_yield": None if pd.isna(row["dividend_yield"]) else round(float(row["dividend_yield"]), 4),
                },
                "component_scores": {
                    "pe_ttm": None if pd.isna(row["component_score_pe_ttm"]) else round(float(row["component_score_pe_ttm"]), 4),
                    "pb": None if pd.isna(row["component_score_pb"]) else round(float(row["component_score_pb"]), 4),
                    "ps": None if pd.isna(row["component_score_ps"]) else round(float(row["component_score_ps"]), 4),
                    "dividend_yield": None if pd.isna(row["component_score_dividend_yield"]) else round(float(row["component_score_dividend_yield"]), 4),
                },
                "missing": missing,
            }
            output_rows.append(
                {
                    "run_date": resolved_run_date,
                    "symbol_id": int(row["symbol_id"]),
                    "score": round(float(row["score"]), 4),
                    "rank": int(row["rank"]),
                    "reasons": reasons,
                }
            )

        return RankingResult(
            run_date=resolved_run_date,
            table=self.table_name,
            rows=output_rows,
            top20=latest.head(20).to_dict(orient="records"),
        )
