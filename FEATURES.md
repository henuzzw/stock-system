# Stock Web / Quant System — Feature Checklist

> Status legend: ✅ done · 🟡 defined/in progress · ❌ not started

## A. Data layer (stock-pipeline / MySQL)
- A1 Daily bars ingest (`daily_prices`) ✅
- A2 Technical indicators (`technicals`: SMA/EMA/RSI/MACD) ✅
- A3 Fundamentals/valuation (`fundamentals`: PE/PB/PS; enriched via `ak.stock_value_em`) ✅
- A4 Candidate pool storage (`candidates_daily` with `rank_left`/`rank_right`) ✅
- A5 Minute bars storage (`minute_prices`, 1m K; supports 10:10/14:50) ✅ (manual CLI tested)
- A6 Automated minute-bar collection (trading days; prioritize candidate symbols) ❌
- A7 Health/alerting (heartbeat log scans, MySQL connectivity checks) ✅

## B. Research layer (stock-research)
- B1 Technical-low ranking (`technical_low_daily`) ✅
- B2 Valuation-low ranking (`valuation_low_daily`) ✅
- B3 Candidate pool signals (`candidates_daily` left/right) ✅
- B4 Candidate ranks stored (`rank_left`/`rank_right`) ✅
- B5 Trend/Risk/Momentum scores (`scores_daily`, `trend_ok` 2/3 gate + `total_score`) ✅
- B6 Use `trend_ok` + `total_score` in buy allocation 🟡
- B7 Backtest comparison for trend-gate variants ❌ (optional)

## C. Strategy rules (agreed)
- C1 Universe: left top3 + right top3 (independent), dedupe; if <6, extend ranks but only within `trend_ok=1` 🟡
- C2 Buy price: 10:10 minute price 🟡
- C3 Daily budget: 100,000 total 🟡
- C4 Allocation: weight by `total_score` (higher => more buy) 🟡
- C5 If `trend_ok` insufficient: hold cash (do not force 6) 🟡
- C6 Sell rule: (i) out of top10 for 2 consecutive days OR (ii) -8% hard stop; sell at 14:50 minute price 🟡
- C7 Orders: support limit orders and potential failure (too low/high) 🟡

## D. Paper-trading / account system
- D1 User register (username+password, BCrypt) ✅
- D2 JWT login/auth (multi-user concurrent) ✅
- D3 Account per user (initial cash 10,000,000) ✅
- D4 Positions (partial buys/sells, cost basis, PnL) ✅
- D5 Orders (market/limit; state machine) 🟡 schema + query API skeleton landed
- D6 Trades (fills; link to orders; audit) 🟡 schema + query API skeleton landed
- D7 Matching engine uses minute bars (10:10/14:50) ❌
- D8 Fees model (optional) ❌
- D9 Equity/PnL curve stats ❌
- D10 Strategy run logs + daily plans (Top3 automation) 🟡 schema + query API skeleton landed

## E. Web UI (port 30000)
- E1 Dashboard (technical/valuation/candidates) ✅
- E2 Candidate filters (triggered/intersection/sort) ✅
- E3 Symbol detail (close + SMA20/60 + fundamentals + candidate history) ✅
- E4 Symbols full search ✅
- E5 Register/Login pages ❌
- E6 Account page (cash/positions/orders/trades/equity) ❌
- E7 Strategy page (daily plan, fills, sell triggers) ❌
- E8 Export CSV/Excel (optional) ❌

## F. Ops
- F1 Nginx on 30000 (SPA + /api proxy) ✅
- F2 Backend Spring Boot systemd auto-start ✅
- F3 Frontend build deploy to nginx root ✅
- F4 Scheduler for minute-bars + strategy execution ❌
- F5 Alerting coverage for trading/strategy (extend heartbeat) ❌

## Notes
- MySQL: 127.0.0.1:3310, db `stock_pipeline`, user/pass `flowing`.
- Nginx conf: `/etc/nginx/conf.d/market_reports.conf`.
- Backend service: `stock-web-backend.service`.
