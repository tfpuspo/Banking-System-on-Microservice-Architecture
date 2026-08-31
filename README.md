

# Banking System — Microservices Architecture

A banking system built on GraphQL, gRPC, Kong API Gateway, and Postgres — one isolated database per service.

**Status: 🟢 Phases 1, 2, and 3 are complete and running** — infrastructure (Postgres × 5, Kong gateway) is up and verified locally. Service implementation (auth, accounts, transactions, ledger, notifications) is in progress.

## Architecture

<img width="2720" height="1760" alt="banking_system_architecture_v2" src="https://github.com/user-attachments/assets/d6a34201-d194-4e70-8d8e-f802994808c9" />

- Clients talk **only** to a GraphQL layer behind Kong.
- Kong routes to backend microservices over **gRPC**.
- Microservices talk to **each other** only over gRPC — never REST, never shared databases.
- `ledger-service` is the single source of truth for money movement; `transaction-service` orchestrates but never writes balances directly.


## Project status

### ✅ Phase 1 — Foundation & infra (done)
- [x] `.proto` contracts written for all 5 services ([`proto/`](./proto))
- [x] 5 isolated Postgres databases running via Docker Compose
- [x] Kong running in declarative mode, verified end-to-end with a proxied test route

### 🔜 Phase 2 — Auth service (next)
- [ ] Implement `auth-service` gRPC server (register, login, token issuance)
- [ ] Connect to `auth-db`
- [ ] Verify with `grpcurl`, no gateway involved

### Planned after that
- Phase 3: Authorization (roles/permissions on `auth-service`)
- Phase 4: Inter-service gRPC (a second service calling `auth-service`)
- Phase 5: Kong + GraphQL gateway wired to real services
- Phase 6: `account-service`, `transaction-service`, `ledger-service`, `notification-service`
- Phase 7: Observability, mTLS, rate limiting, audit logging

<img width="1366" height="768" alt="UI" src="https://github.com/user-attachments/assets/fb7a5482-317c-46aa-8c16-e2a040821c24" />

<img width="1366" height="765" alt="doc1" src="https://github.com/user-attachments/assets/f97a9158-b073-4a44-b1a4-6088b9f94e2f" />

<img width="1366" height="768" alt="doc2" src="https://github.com/user-attachments/assets/74ac3408-29e7-4129-8d84-3c6d4b2cca18" />

