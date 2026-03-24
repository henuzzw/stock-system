# HANDOFF — Stock Quant System

Last updated: 2026-03-24 (Asia/Shanghai)

## What is running
- Nginx serves SPA on port 30000, proxies `/api/` to Spring Boot.
  - Nginx conf: `/etc/nginx/conf.d/market_reports.conf`
  - Frontend build output deployed to: `/var/www/stock-web`
  - Requested mirror deploy target for this session: `/home/openclaw/.openclaw/workspace/reports` (copy blocked by sandbox permissions)
- Spring Boot backend (port 8088) is managed by systemd:
  - Service: `stock-web-backend.service` (enabled)
  - Health: `curl http://127.0.0.1:30000/api/health`
  - D6 update on 2026-03-24: rebuilt jar deployed in place and service was bounced by terminating the managed Java process; systemd respawned it on the new jar
- Stock research catch-up is managed by user-level systemd for `openclaw`:
  - Timer: `stock-research-catchup.timer` (enabled)
  - Service: `stock-research-catchup.service`
  - Unit dir: `/home/openclaw/.config/systemd/user`
  - Log: `/tmp/stock-research-timer.log`
  - Behavior: every 30 minutes after boot, plus weekday 15:40 schedule; script no-ops unless research tables lag the latest `daily_prices.trade_date`

## Key projects
- Data pipeline: `/home/openclaw/projects/stock-system/services/stock-pipeline`
  - MySQL: 127.0.0.1:3310 db `stock_pipeline` user/pass `flowing`
  - Added minute bars table `minute_prices` and CLI command:
    - `stock-pipeline minute --codes 601985 --date 2026-03-10`
  - `stock-pipeline intraday` refreshes daily data and snapshots only; it does not write `minute_prices`
  - Dedicated candidate minute-bar collection now runs via `stock-pipeline minute-candidates`
- Research: `/home/openclaw/projects/stock-system/services/stock-research`
  - Tables: `technical_low_daily`, `valuation_low_daily`, `candidates_daily (rank_left/rank_right)`, `scores_daily (trend_ok + total_score)`
  - Trend gate: 2-of-3 (close>SMA20, SMA20 non-down, no_new_low_10)
- Web backend: `/home/openclaw/projects/stock-system/apps/stock-web-backend`
  - API endpoints: /api/health, /api/ranks/*, /api/candidates (supports filters), /api/symbol/{code}, /api/symbols, /api/orders, /api/trades
  - Trade APIs: `GET /api/trades`, `GET /api/trades/{id}`; both reuse Bearer JWT auth and scope queries to the current user
- Web frontend: `/home/openclaw/projects/stock-system/apps/stock-web-frontend`
  - Added standalone trades page: `/trades`

## Agreed trading strategy (config)
- Strategy config file: `/home/openclaw/projects/stock-system/strategy.yml`
- Strategy baseline has been updated from fixed-time execution to intraday signal-driven execution.
- Buy: daily budget 100,000; pick 6 symbols from candidates (left top3 + right top3), dedupe; require `trend_ok=1`, price above `SMA20`, and apply simple overheat filter (for example RSI / intraday gain guard).
- Buy execution: use the nearest available minute bar around the signal trigger during the trading session; do not hard-code 10:10.
- Sell execution: use the nearest available minute bar around the trigger during the trading session; do not hard-code 14:50.
- Sell rules: hard stop -8%; out of top10 for 2 consecutive days (same pool); trend failure (`trend_ok=0` or close below `SMA20` for 2 days); trailing take-profit after sufficient unrealized gain.
- Allocation: pool split 50/50; weight by total_score * (1/rank), with per-position risk cap.

## Feature checklist
- Canonical checklist: `/home/openclaw/projects/stock-system/FEATURES.md`

## Monorepo path note
- In this consolidated repo, the backend jar to build/run is the Spring Boot app under `/home/openclaw/projects/stock-system/apps/stock-web-backend`.

## Immediate next steps (not done yet)
1) Finish the remaining paper-trading/account surfaces: equity/PnL curve stats, fuller account dashboard consolidation, fees, and matching edge cases.
2) Improve minute-bar reliability for Eastmoney provider failures on batch runs.
3) Strategy executor + daily plan/trade logs using `strategy.yml`.
4) Frontend pages: register/login, account dashboard, strategy dashboard.

## D6 note
- Backend trade reads now use a dedicated controller/service/repository/mapper path under `apps/stock-web-backend`.
- Trade model now carries `id`, `userId`, `orderId`, `strategyRunId`, `symbolId`, `side`, `quantity`, `price`, `amount`, `createdAt`, `code`, and `name`.
- Existing D5 order APIs were left unchanged.
- Sandbox limits in this session prevented direct host-side `curl` and MySQL socket checks from inside Codex, so ownership/API verification should be rerun from an unsandboxed shell if needed.

## Minute-Bar Issue Note
- Symptom: some symbol detail pages return `intraday: []` while daily prices, fundamentals, technicals, and candidates are present.
- Root cause: `stock-pipeline intraday` never writes `minute_prices`; detail-page intraday data is read from `minute_prices` only.
- Current operational handling:
  - One-off backfill: `stock-pipeline minute --codes <CODE> --date <YYYY-MM-DD>`
  - Scheduled collection: `stock-pipeline minute-candidates`
  - Minute-bar jobs should clear proxy env vars before calling Eastmoney endpoints.
- Current limitation: Eastmoney minute endpoints still fail intermittently with DNS / `RemoteDisconnected` / connection errors for some symbols. Batch collection now logs per-symbol failures and continues, but the upstream provider is not fully stable.
- Verified example: `601868` was backfilled for `2026-03-19` with 241 rows covering `09:30` to `15:00`.

## After reboot — quick verification
1) `systemctl status nginx` and `systemctl status stock-web-backend`
2) `XDG_RUNTIME_DIR=/run/user/1000 DBUS_SESSION_BUS_ADDRESS=unix:path=/run/user/1000/bus systemctl --user status stock-research-catchup.timer`
3) `curl http://127.0.0.1:30000/api/health`
4) Open http://180.76.138.67:30000/

## Sudo helper
- Use `/home/openclaw/secure_sudo_helper.sh run <cmd>` for privileged ops.
