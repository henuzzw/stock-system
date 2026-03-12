from __future__ import annotations

from datetime import date
from typing import Any

import pandas as pd

from stock_research.db import Database


def fetch_candidates(db: Database, run_date: date) -> pd.DataFrame:
    rows = db.fetchall(
        """
        SELECT c.run_date, c.symbol_id, s.code, s.name,
               c.in_technical_top20, c.in_valuation_top20,
               c.left_triggered, c.left_signal_score,
               c.right_triggered, c.right_signal_score,
               c.snapshot
        FROM candidates_daily c
        JOIN symbols s ON s.id=c.symbol_id
        WHERE c.run_date = %s
        """,
        (run_date.isoformat(),),
    )
    return pd.DataFrame(rows)


def render_candidate_top(df: pd.DataFrame, *, side: str, topn: int = 20) -> str:
    if df.empty:
        return "No candidates."

    if side == "left":
        sort_cols = ["left_signal_score", "right_signal_score"]
        df2 = df.sort_values(sort_cols, ascending=[False, False]).head(topn)
        title = f"candidates_left top {topn}"
    else:
        sort_cols = ["right_signal_score", "left_signal_score"]
        df2 = df.sort_values(sort_cols, ascending=[False, False]).head(topn)
        title = f"candidates_right top {topn}"

    lines = [title]
    for idx, row in enumerate(df2.to_dict(orient="records"), 1):
        lines.append(
            f"{idx:>2}  {row['code']:<12} {row['name']:<20} "
            f"L={float(row['left_signal_score']):6.2f}({int(row['left_triggered'])}) "
            f"R={float(row['right_signal_score']):6.2f}({int(row['right_triggered'])}) "
            f"T={int(row['in_technical_top20'])} V={int(row['in_valuation_top20'])}"
        )
    return "\n".join(lines)
