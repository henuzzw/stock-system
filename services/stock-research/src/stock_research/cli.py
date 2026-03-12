from __future__ import annotations

import argparse
from datetime import datetime

from stock_research.config import load_settings
from stock_research.db import Database
from stock_research.candidates_rank import fetch_candidates, render_candidate_top
from stock_research.cli_scores import render_scores_top
from stock_research.service import ResearchService, render_top20


def _parse_date(value: str | None):
    if not value:
        return None
    return datetime.strptime(value, "%Y-%m-%d").date()


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Stock research CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("init-db", help="Create ranking output tables")

    technical = subparsers.add_parser("run-technical", help="Compute technical low rankings")
    technical.add_argument("--run-date", help="Optional run date in YYYY-MM-DD")

    valuation = subparsers.add_parser("run-valuation", help="Compute valuation low rankings")
    valuation.add_argument("--run-date", help="Optional run date in YYYY-MM-DD")

    run_all = subparsers.add_parser("run-all", help="Compute both ranking sets")
    run_all.add_argument("--run-date", help="Optional run date in YYYY-MM-DD")

    candidates = subparsers.add_parser("run-candidates", help="Build candidates_daily from top20 sets and signals")
    candidates.add_argument("--run-date", help="Optional run date in YYYY-MM-DD")

    cand_top = subparsers.add_parser("candidates-top", help="Print candidates top20 lists (left/right)")
    cand_top.add_argument("--side", choices=["left", "right"], default="left")
    cand_top.add_argument("--run-date", help="Optional run date in YYYY-MM-DD")

    scores_top = subparsers.add_parser("scores-top", help="Print scores_daily top list")
    scores_top.add_argument("--run-date", help="Optional run date in YYYY-MM-DD")
    scores_top.add_argument("--top", type=int, default=20)
    scores_top.add_argument("--trend-ok-only", action="store_true")

    return parser


def main() -> None:
    args = build_parser().parse_args()
    service = ResearchService(Database(load_settings()))
    run_date = _parse_date(getattr(args, "run_date", None))

    if args.command == "init-db":
        service.init_database()
        print("Created tables: technical_low_daily, valuation_low_daily")
        return

    if args.command == "run-technical":
        result = service.run_technical(run_date=run_date)
        print(render_top20(result))
        return

    if args.command == "run-valuation":
        result = service.run_valuation(run_date=run_date)
        print(render_top20(result))
        return

    if args.command == "run-all":
        for result in service.run_all(run_date=run_date):
            print(render_top20(result))
            print()
        return

    if args.command == "run-candidates":
        info = service.run_candidates(run_date=run_date)
        print(
            f"candidates_daily for {info['run_date'].isoformat()}: total={info['count']} left_triggered={info['left_triggered']} right_triggered={info['right_triggered']}"
        )
        return

    if args.command == "candidates-top":
        # assumes candidates_daily already populated for the run_date
        db = Database(load_settings())
        df = fetch_candidates(db, run_date or datetime.now().date())
        print(render_candidate_top(df, side=args.side, topn=20))
        return

    if args.command == "scores-top":
        db = Database(load_settings())
        rd = run_date or datetime.now().date()
        print(render_scores_top(db, rd, topn=args.top, only_trend_ok=args.trend_ok_only))
        return


if __name__ == "__main__":
    main()
