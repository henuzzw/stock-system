# Stock Pipeline

Python project to ingest `/home/openclaw/.openclaw/workspace/data/watchlist.csv` into MySQL and keep market data current for A-shares via `akshare` + `baostock` and US symbols via `yfinance`.

## Features

- Imports the watchlist into a `symbols` table.
- Loads daily prices, fundamentals, news, technical indicators, and intraday snapshots.
- Uses MySQL upserts so daily tables are idempotent on `(symbol_id, trade_date)` and snapshots on `(symbol_id, snapshot_time)`.
- Records provider/source on every market-data table and writes execution status to `ingestion_log`.
- Runs without API keys (AkShare + Baostock + yfinance).

## Project Layout

```text
migrations/001_init.sql
src/stock_pipeline/
  cli.py
  config.py
  db.py
  ingest.py
  providers.py
  scheduler.py
  watchlist.py
```

## Requirements

- Python 3.10+
- MySQL reachable at `127.0.0.1`
- Default credentials: user `flowing`, password `flowing`
- Port defaults to `3306`; override with `MYSQL_PORT`
- Packages: akshare, baostock, yfinance

## Setup

```bash
python -m venv .venv
source .venv/bin/activate
pip install -e .
cp .env .env.local  # optional local override
```

Create the database schema:

```bash
mysql -h 127.0.0.1 -P "${MYSQL_PORT:-3306}" -u flowing -pflowing < migrations/001_init.sql
```

Or via the CLI:

```bash
stock-pipeline init-db
```

Load the watchlist:

```bash
stock-pipeline import-watchlist
```

Run a full ingestion:

```bash
stock-pipeline ingest --start-date 2025-01-01 --end-date 2026-03-10
```

Run a focused US-symbol pull:

```bash
stock-pipeline ingest --codes EH
```

## Configuration

The included `.env` file contains defaults:

```dotenv
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
MYSQL_USER=flowing
MYSQL_PASSWORD=flowing
MYSQL_DATABASE=stock_pipeline
WATCHLIST_PATH=/home/openclaw/.openclaw/workspace/data/watchlist.csv
INTRADAY_INTERVAL_MINUTES=15
DEFAULT_LOOKBACK_DAYS=180
```

## Scheduler

The scheduler polls every 15-30 minutes during market hours and triggers one close recap after the A-share close window.

Single intraday run:

```bash
stock-pipeline intraday
```

Daily recap:

```bash
stock-pipeline daily-close
```

Continuous scheduler:

```bash
stock-pipeline schedule --interval-minutes 15
```

### Cron examples

Every 15 minutes on weekdays:

```cron
*/15 * * * 1-5 cd /home/openclaw/projects/stock-pipeline && /home/openclaw/projects/stock-pipeline/.venv/bin/stock-pipeline intraday >> /tmp/stock-pipeline-intraday.log 2>&1
```

Daily close recap for A-shares at 15:20 Asia/Shanghai weekdays:

```cron
20 15 * * 1-5 cd /home/openclaw/projects/stock-pipeline && /home/openclaw/projects/stock-pipeline/.venv/bin/stock-pipeline daily-close >> /tmp/stock-pipeline-close.log 2>&1
```

US close recap at 16:20 America/New_York weekdays:

```cron
20 16 * * 1-5 cd /home/openclaw/projects/stock-pipeline && TZ=America/New_York /home/openclaw/projects/stock-pipeline/.venv/bin/stock-pipeline daily-close --codes EH >> /tmp/stock-pipeline-us-close.log 2>&1
```

## Notes

- `akshare` is the primary CN source; `baostock` is used as a fallback for daily K data if AkShare fails.
- `efinance` is used as a fallback for CN real-time snapshot quotes when AkShare snapshots fail.
- `akshare` coverage varies by endpoint; the pipeline falls back gracefully when provider news endpoints fail.
- Technical indicators are derived from `daily_prices` and stored in `technicals`.
- For repeated runs, `INSERT ... ON DUPLICATE KEY UPDATE` keeps writes idempotent.
