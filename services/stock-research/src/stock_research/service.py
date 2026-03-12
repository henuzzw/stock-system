from __future__ import annotations

from datetime import date
from typing import Any

from stock_research.analytics import RankingResult, TechnicalLowRanker, ValuationLowRanker
from stock_research.candidates import build_candidates
from stock_research.scores import build_scores
from stock_research.db import Database
from stock_research.repository import ResearchRepository
from stock_research.sql import INIT_SQL


class ResearchService:
    def __init__(self, db: Database) -> None:
        self.db = db
        self.repository = ResearchRepository(db)
        self.technical_ranker = TechnicalLowRanker()
        self.valuation_ranker = ValuationLowRanker()

    def init_database(self) -> None:
        self.db.execute_script(INIT_SQL)

    def run_technical(self, run_date: date | None = None) -> RankingResult:
        result = self.technical_ranker.build_rankings(
            self.repository.fetch_technical_source_rows(),
            run_date=run_date,
        )
        self._persist_result(result)
        return result

    def run_valuation(self, run_date: date | None = None) -> RankingResult:
        result = self.valuation_ranker.build_rankings(
            self.repository.fetch_valuation_source_rows(),
            run_date=run_date,
        )
        self._persist_result(result)
        return result

    def run_candidates(self, run_date: date | None = None) -> dict[str, Any]:
        technical = self.run_technical(run_date=run_date)
        valuation = self.run_valuation(run_date=run_date)
        run_date_resolved = run_date or technical.run_date

        tech_top = [row for row in technical.top20]
        val_top = [row for row in valuation.top20]

        import pandas as pd

        technical_top_df = (
            pd.DataFrame([{"symbol_id": r["symbol_id"]} for r in tech_top])
            if tech_top
            else pd.DataFrame(columns=["symbol_id"])
        )
        valuation_top_df = (
            pd.DataFrame([{"symbol_id": r["symbol_id"]} for r in val_top])
            if val_top
            else pd.DataFrame(columns=["symbol_id"])
        )

        tech_components = pd.DataFrame(self.repository.fetch_latest_technical_components())
        val_components = pd.DataFrame(self.repository.fetch_latest_valuation_components())

        rows = build_candidates(
            run_date=run_date_resolved,
            technical_top=technical_top_df,
            valuation_top=valuation_top_df,
            technical_components=tech_components,
            valuation_components=val_components,
        )
        if rows:
            self.db.upsert_rows(
                "candidates_daily",
                rows,
                key_columns=["run_date", "symbol_id"],
                update_columns=[
                    "in_technical_top20",
                    "in_valuation_top20",
                    "left_signal_score",
                    "right_signal_score",
                    "left_triggered",
                    "right_triggered",
                    "rank_left",
                    "rank_right",
                    "snapshot",
                ],
            )

            # compute daily ranks within the candidates universe
            import pandas as pd

            df = pd.DataFrame(rows)
            df = df.sort_values(["left_signal_score", "right_signal_score"], ascending=[False, False]).reset_index(drop=True)
            df["rank_left"] = df.index + 1
            df = df.sort_values(["right_signal_score", "left_signal_score"], ascending=[False, False]).reset_index(drop=True)
            df["rank_right"] = df.index + 1

            rank_rows = df[["run_date", "symbol_id", "rank_left", "rank_right"]].to_dict(orient="records")
            # Use direct UPDATEs to avoid INSERT missing NOT NULL fields
            for rr in rank_rows:
                self.db.execute(
                    "UPDATE candidates_daily SET rank_left=%s, rank_right=%s WHERE run_date=%s AND symbol_id=%s",
                    (rr["rank_left"], rr["rank_right"], rr["run_date"], rr["symbol_id"]),
                )

        return {
            "run_date": run_date_resolved,
            "count": len(rows),
            "left_triggered": sum(1 for r in rows if r["left_triggered"] == 1),
            "right_triggered": sum(1 for r in rows if r["right_triggered"] == 1),
        }

    def run_scores(self, run_date: date | None = None) -> dict[str, Any]:
        import pandas as pd

        # Use latest available trade_date as run_date default
        dp = pd.DataFrame(
            self.db.fetchall(
                "SELECT symbol_id, trade_date, open, high, low, close, volume, turnover FROM daily_prices ORDER BY symbol_id, trade_date"
            )
        )
        tech = pd.DataFrame(
            self.db.fetchall(
                "SELECT symbol_id, trade_date, sma_20, rsi_14 FROM technicals ORDER BY symbol_id, trade_date"
            )
        )
        if dp.empty:
            rd = run_date or date.today()
            return {"run_date": rd, "count": 0, "trend_ok": 0}

        rd = run_date or pd.to_datetime(dp["trade_date"]).max().date()
        result = build_scores(run_date=rd, daily_prices=dp, technicals=tech)

        if result.rows:
            self.db.upsert_rows(
                "scores_daily",
                result.rows,
                key_columns=["run_date", "symbol_id"],
                update_columns=[
                    "trend_ok",
                    "trend_score",
                    "risk_score",
                    "momentum_score",
                    "total_score",
                    "reasons",
                ],
            )

        return {
            "run_date": rd,
            "count": len(result.rows),
            "trend_ok": sum(1 for r in result.rows if r["trend_ok"] == 1),
        }

    def run_all(self, run_date: date | None = None) -> list[RankingResult]:
        results = [self.run_technical(run_date=run_date), self.run_valuation(run_date=run_date)]
        # also materialize candidates table and scores as part of run-all
        self.run_candidates(run_date=run_date)
        self.run_scores(run_date=run_date)
        return results

    def _persist_result(self, result: RankingResult) -> None:
        if not result.rows:
            return
        self.db.upsert_rows(
            result.table,
            result.rows,
            key_columns=["run_date", "symbol_id"],
            update_columns=["score", "rank", "reasons"],
        )


def render_top20(result: RankingResult) -> str:
    lines = [f"{result.table} top 20 for {result.run_date.isoformat()}"]
    if not result.top20:
        lines.append("No rows.")
        return "\n".join(lines)

    for row in result.top20:
        lines.append(
            f"{int(row['rank']):>2}  {row['code']:<12} {row['name']:<20} score={float(row['score']):6.2f}"
        )
    return "\n".join(lines)
