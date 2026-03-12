from __future__ import annotations

import argparse
from datetime import date, datetime
from pathlib import Path

from stock_pipeline.config import load_settings
from stock_pipeline.ingest import DataIngestor
from stock_pipeline.minute_ingest import fetch_minute_bars_akshare, upsert_minute_prices
from stock_pipeline.scheduler import run_loop


def _parse_date(value: str | None) -> date | None:
    if not value:
        return None
    return datetime.strptime(value, "%Y-%m-%d").date()


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Stock pipeline CLI")
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("import-watchlist", help="Load watchlist.csv into the symbols table")

    init_db = subparsers.add_parser("init-db", help="Apply the initial SQL migration")
    init_db.add_argument(
        "--migration",
        default=str(Path("migrations/001_init.sql")),
        help="Path to SQL migration file",
    )

    ingest = subparsers.add_parser("ingest", help="Fetch and ingest market data")
    ingest.add_argument("--codes", nargs="*", help="Optional symbol codes to ingest")
    ingest.add_argument("--start-date", help="Inclusive start date in YYYY-MM-DD")
    ingest.add_argument("--end-date", help="Inclusive end date in YYYY-MM-DD")
    ingest.add_argument("--skip-news", action="store_true", help="Skip news ingestion")
    ingest.add_argument("--skip-snapshots", action="store_true", help="Skip snapshot ingestion")

    intraday = subparsers.add_parser("intraday", help="Run a single intraday refresh")
    intraday.add_argument("--codes", nargs="*", help="Optional symbol codes to ingest")

    recap = subparsers.add_parser("daily-close", help="Run daily close recap")
    recap.add_argument("--codes", nargs="*", help="Optional symbol codes to ingest")

    minute = subparsers.add_parser("minute", help="Fetch and ingest 1-minute bars for A-shares (AkShare)")
    minute.add_argument("--codes", nargs="*", help="Optional symbol codes to ingest")
    minute.add_argument("--date", help="Trading date in YYYY-MM-DD (default today)")

    schedule = subparsers.add_parser("schedule", help="Run the scheduler loop")
    schedule.add_argument(
        "--interval-minutes",
        type=int,
        default=None,
        help="Polling interval in minutes; defaults to INTRADAY_INTERVAL_MINUTES",
    )

    return parser


def main() -> None:
    parser = build_parser()
    args = parser.parse_args()
    settings = load_settings()
    ingestor = DataIngestor(settings)

    if args.command == "init-db":
        ingestor.init_database(Path(args.migration))
        print("Applied migration:", args.migration)
        return

    if args.command == "import-watchlist":
        count = ingestor.import_watchlist()
        print(f"Imported watchlist rows: {count}")
        return

    if args.command == "ingest":
        count = ingestor.ingest_market_data(
            codes=args.codes,
            start_date=_parse_date(args.start_date),
            end_date=_parse_date(args.end_date),
            include_news=not args.skip_news,
            include_snapshots=not args.skip_snapshots,
        )
        print(f"Rows written: {count}")
        return

    if args.command == "intraday":
        count = ingestor.intraday_update(codes=args.codes)
        print(f"Intraday rows written: {count}")
        return

    if args.command == "daily-close":
        count = ingestor.daily_close_recap(codes=args.codes)
        print(f"Daily close rows written: {count}")
        return

    if args.command == "minute":
        from stock_pipeline.db import Database
        from stock_pipeline.watchlist import WatchlistSymbol
        from datetime import date as _date

        target_date = _parse_date(args.date) or _date.today()
        db = Database(settings)
        symbols = ingestor.get_symbols(args.codes)
        total = 0
        for row in symbols:
            if row["market"] != "CN":
                continue
            sym = WatchlistSymbol(
                code=row["code"],
                name=row["name"],
                market=row["market"],
                exchange=row["exchange"],
                source=row["source"],
            )
            bars = fetch_minute_bars_akshare(sym, target_date)
            total += upsert_minute_prices(db, row["id"], bars)
        print(f"Minute rows written: {total}")
        return

    if args.command == "schedule":
        run_loop(settings, poll_minutes=args.interval_minutes)
        return


if __name__ == "__main__":
    main()
