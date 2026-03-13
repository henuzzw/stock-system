# HANDOFF — Stock Quant System

Last updated: 2026-03-10 (Asia/Shanghai)

## What is running
- Nginx serves SPA on port 30000, proxies `/api/` to Spring Boot.
  - Nginx conf: `/etc/nginx/conf.d/market_reports.conf`
  - Frontend build output deployed to: `/home/openclaw/.openclaw/workspace/reports`
- Spring Boot backend (port 8088) is managed by systemd:
  - Service: `stock-web-backend.service` (enabled)
  - Health: `curl http://127.0.0.1:30000/api/health`

## Key projects
- Data pipeline: `/home/openclaw/projects/stock-pipeline`
  - MySQL: 127.0.0.1:3310 db `stock_pipeline` user/pass `flowing`
  - Added minute bars table `minute_prices` and CLI command:
    - `stock-pipeline minute --codes 601985 --date 2026-03-10`
- Research: `/home/openclaw/projects/stock-research`
  - Tables: `technical_low_daily`, `valuation_low_daily`, `candidates_daily (rank_left/rank_right)`, `scores_daily (trend_ok + total_score)`
  - Trend gate: 2-of-3 (close>SMA20, SMA20 non-down, no_new_low_10)
- Web backend: `/home/openclaw/projects/stock-system/apps/stock-web-backend`
  - API endpoints: /api/health, /api/ranks/*, /api/candidates (supports filters), /api/symbol/{code}, /api/symbols
- Web frontend: `/home/openclaw/projects/stock-system/apps/stock-web-frontend`

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
1) Paper-trading/account system (users/JWT/accounts/orders/trades/positions/equity curves).
2) Automated minute-bar collection (cron) for candidate symbols.
3) Strategy executor + daily plan/trade logs using `strategy.yml`.
4) Frontend pages: register/login, account dashboard, strategy dashboard.

## After reboot — quick verification
1) `systemctl status nginx` and `systemctl status stock-web-backend`
2) `curl http://127.0.0.1:30000/api/health`
3) Open http://180.76.138.67:30000/

## Sudo helper
- Use `/home/openclaw/secure_sudo_helper.sh run <cmd>` for privileged ops.
