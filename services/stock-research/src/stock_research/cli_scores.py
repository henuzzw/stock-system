from __future__ import annotations

from datetime import date

from stock_research.db import Database


def render_scores_top(db: Database, run_date: date, topn: int = 20, only_trend_ok: bool = False) -> str:
    where = "WHERE r.run_date = %s"
    params = [run_date.isoformat()]
    if only_trend_ok:
        where += " AND r.trend_ok = 1"

    rows = db.fetchall(
        f"""
        SELECT s.code, s.name, r.total_score, r.trend_score, r.risk_score, r.momentum_score, r.trend_ok
        FROM scores_daily r
        JOIN symbols s ON s.id = r.symbol_id
        {where}
        ORDER BY r.total_score DESC
        LIMIT {int(topn)}
        """,
        tuple(params),
    )

    lines = [f"scores_daily top {topn} for {run_date.isoformat()} (trend_ok_only={only_trend_ok})"]
    for i, r in enumerate(rows, 1):
        lines.append(
            f"{i:>2} {r['code']:<10} {r['name']:<16} total={float(r['total_score']):6.2f} trend={float(r['trend_score']):6.2f} risk={float(r['risk_score']):6.2f} mom={float(r['momentum_score']):6.2f} ok={int(r['trend_ok'])}"
        )
    return "\n".join(lines)
