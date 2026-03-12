# PROJECT — Stock Web Backend

Last updated: 2026-03-11 (Asia/Shanghai)

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

## Backend structure
- BCrypt helper: `stock-web-backend/src/main/java/ai/openclaw/stockweb/auth/BcryptPasswordHasher.java`
- Controller: `stock-web-backend/src/main/java/ai/openclaw/stockweb/api/AuthController.java`
- Controller: `stock-web-backend/src/main/java/ai/openclaw/stockweb/api/AccountController.java`
- Service: `stock-web-backend/src/main/java/ai/openclaw/stockweb/auth/AuthService.java`
- Repositories:
  - `stock-web-backend/src/main/java/ai/openclaw/stockweb/auth/UserRepository.java`
  - `stock-web-backend/src/main/java/ai/openclaw/stockweb/account/AccountRepository.java`

## Deferred / blocked in this session
- Frontend account page was requested but the frontend workspace (`/home/openclaw/projects/stock-web-frontend`) is outside this session's writable roots, so it could not be edited from the current sandbox.
