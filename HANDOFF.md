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
- Web backend: `/home/openclaw/projects/stock-web-backend`
  - API endpoints: /api/health, /api/ranks/*, /api/candidates (supports filters), /api/symbol/{code}, /api/symbols
- Web frontend: `/home/openclaw/projects/stock-web-frontend`

## Agreed trading strategy (config)
- Strategy config file: `/home/openclaw/projects/stock-web-backend/strategy.yml`
- Buy: daily budget 100,000; pick 6 symbols from candidates (left top3 + right top3), dedupe; only `trend_ok=1`, else hold cash.
- Buy price time: 10:10 minute bar close.
- Sell: 14:50 minute bar close.
- Sell rules: out of top10 for 2 consecutive days (same pool) OR hard stop -8%.
- Allocation: pool split 50/50; weight by total_score * (1/rank).

## Feature checklist
- Canonical checklist: `/home/openclaw/projects/stock-web-backend/FEATURES.md`

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
