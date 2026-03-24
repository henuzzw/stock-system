# PROJECT — Stock Web Backend

Last updated: 2026-03-24 (Asia/Shanghai)

## D1 implemented
- Added `users` and `accounts` bootstrap schema in Spring Boot `schema.sql`.
- `POST /api/auth/register` now accepts `username` and `password`.
- Validation rules:
  - `username` must be non-empty after trim.
  - `password` must be at least 8 characters.
- Passwords are stored only as BCrypt hashes via the backend BCrypt hasher.
- Registration creates a default `accounts` row with:
  - `initial_cash = 10000000.00`
  - `cash_balance = 10000000.00`

## D2 implemented
- Added `POST /api/auth/login` for username/password login.
- Login verifies `users.password_hash` with BCrypt before issuing a JWT.
- Added minimal JWT utility with HS256 signing using `auth.jwt.secret`.
- Added `GET /api/auth/me` which returns current user basic info for a valid `Authorization: Bearer <token>`.
- Existing non-auth APIs remain unchanged and are not globally secured yet.

## D4 implemented
- Added `positions` bootstrap schema in Spring Boot `schema.sql`.
- Added protected `GET /api/account/positions` using the existing Bearer token/JWT auth path.
- Positions API returns the current user's positions joined with `symbols` (`code`, `name`).
- Added protected `GET /api/account/summary`.
- Summary API returns `initial_cash`, `cash_balance`, `positions_count`, and aggregate `positions_market_value` using each symbol's latest `daily_prices.close`.

## D5 implemented
- `GET /api/orders` and `GET /api/orders/{id}` remain on the dedicated order controller/service/repository/mapper path.
- Manual order placement behavior was left unchanged for D6.

## D6 implemented
- Added a dedicated trade read stack:
  - `GET /api/trades`
  - `GET /api/trades/{id}`
- Both endpoints reuse the existing Bearer JWT auth flow and only return rows belonging to the authenticated user.
- Introduced `ai.openclaw.stockweb.trade.TradeView` and `TradeService` / `TradeRepository` / `TradeMapper` for the D6 query path.
- Trade rows now expose `id`, `userId`, `orderId`, `strategyRunId`, `symbolId`, `side`, `quantity`, `price`, `amount`, `createdAt`, `code`, and `name`.
- Existing strategy/account trade insert paths were updated to carry `order_id` when available, while keeping existing order behavior unchanged.
- Added standalone frontend page `/trades` with nav entry `成交`, loading the JWT from `localStorage` and prompting login when missing.

## D7 implemented
- Added matching APIs:
  - `POST /api/matching/run`
  - `GET /api/matching/last`
- Matching runs are scoped to the authenticated user and only inspect open `orders.status IN ('NEW', 'PARTIAL')`.
- Price resolution order:
  - latest same-day `minute_prices.close` at or before the current Asia/Shanghai time
  - fallback latest `daily_prices.close`
- Fill rules:
  - `BUY`: requires enough `accounts.cash_balance`
  - `SELL`: requires enough `positions.available_quantity`
  - `LIMIT` orders only fill when the resolved market price satisfies the limit
  - `PARTIAL` orders fill only their remaining quantity and then advance to `FILLED`
- Order execution is atomic per order via `MatchingOrderExecutor` with `REQUIRES_NEW` transactions.
- Added persisted `matching_runs` bootstrap schema so the last summary is queryable after restart.
- Added standalone frontend page `/matching` with a `撮合` nav entry, manual trigger button, immediate run summary, and last-run summary.

## Backend structure
- BCrypt helper: `stock-web-backend/src/main/java/ai/openclaw/stockweb/auth/BcryptPasswordHasher.java`
- Controller: `stock-web-backend/src/main/java/ai/openclaw/stockweb/api/AuthController.java`
- Controller: `stock-web-backend/src/main/java/ai/openclaw/stockweb/api/AccountController.java`
- Controller: `stock-web-backend/src/main/java/ai/openclaw/stockweb/api/OrdersController.java`
- Controller: `stock-web-backend/src/main/java/ai/openclaw/stockweb/api/TradesController.java`
- Controller: `stock-web-backend/src/main/java/ai/openclaw/stockweb/api/MatchingController.java`
- Service: `stock-web-backend/src/main/java/ai/openclaw/stockweb/auth/AuthService.java`
- Service: `stock-web-backend/src/main/java/ai/openclaw/stockweb/order/OrderService.java`
- Service: `stock-web-backend/src/main/java/ai/openclaw/stockweb/trade/TradeService.java`
- Service: `stock-web-backend/src/main/java/ai/openclaw/stockweb/matching/MatchingService.java`
- Repositories:
  - `stock-web-backend/src/main/java/ai/openclaw/stockweb/auth/UserRepository.java`
  - `stock-web-backend/src/main/java/ai/openclaw/stockweb/account/AccountRepository.java`
  - `stock-web-backend/src/main/java/ai/openclaw/stockweb/order/OrderRepository.java`
  - `stock-web-backend/src/main/java/ai/openclaw/stockweb/trade/TradeRepository.java`
  - `stock-web-backend/src/main/java/ai/openclaw/stockweb/matching/MatchingRepository.java`

## Verification / limits
- Backend jar was rebuilt successfully with `./mvnw -DskipTests package`.
- The systemd-managed backend was bounced on 2026-03-24 and restarted on the rebuilt jar.
- Frontend `npm run build` succeeded.
- Copying the frontend build into `/home/openclaw/.openclaw/workspace/reports` was blocked by sandbox filesystem permissions.
- Direct `curl` and MySQL verification from this session was blocked by sandbox socket restrictions, so the D7 runtime verification commands should be rerun from an unsandboxed shell.
