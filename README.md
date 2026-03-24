# stock-system

Monorepo for the stock data / research / web / paper-trading system.

## Structure

- `services/stock-pipeline` — market data ingestion into MySQL
- `services/stock-research` — ranking, candidate pool, and score generation
- `apps/stock-web-backend` — Spring Boot API, auth, account, and trading simulation backend
- `apps/stock-web-frontend` — Vite frontend served behind nginx on port 30000
- `services/stock-research/systemd` — user-level systemd timer/service for resilient research catch-up
- `HANDOFF.md` — operator handoff notes
- `FEATURES.md` — canonical feature checklist
- `PROJECT.md` — backend/project implementation notes
- `strategy.yml` — agreed strategy configuration

## Notes

This repo was assembled from the working project directories on the host so development can continue in one place without changing currently running deployment paths.
