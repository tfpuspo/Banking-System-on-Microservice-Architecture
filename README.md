# Banking System — Microservices Architecture

A banking system built on GraphQL, gRPC, Kong API Gateway, and Postgres — one isolated database per service.

**Status: 🟢 Phase 1 complete and running** — infrastructure (Postgres × 5, Kong gateway) is up and verified locally. Service implementation (auth, accounts, transactions, ledger, notifications) is in progress.

## Architecture

```
Consumers (Web / Mobile / Other)
        │
      GraphQL
        │
        ▼
  API Gateway (Kong)
   JWT check · routing
        │
       gRPC
        │
        ▼
┌─────────────────────────────────────────┐
│              Microservices               │
│                                           │
│   Auth      Account     Transaction      │
│  (own DB)   (own DB)     (own DB)        │
│                                           │
│         Ledger      Notification         │
│        (own DB)       (own DB)           │
└─────────────────────────────────────────┘
```

- Clients talk **only** to a GraphQL layer behind Kong.
- Kong routes to backend microservices over **gRPC**.
- Microservices talk to **each other** only over gRPC — never REST, never shared databases.
- `ledger-service` is the single source of truth for money movement; `transaction-service` orchestrates but never writes balances directly.

Full service responsibilities and communication rules: [`SERVICES.md`](./SERVICES.md)

## Tech stack

### Backend
- **Language/runtime**: Go / Node / Python / Java — TBD, decision pending for Phase 2
- **Service-to-service communication**: gRPC — contracts defined in [`proto/`](./proto)
- **API layer**: GraphQL — single query layer exposed to clients, no REST endpoints
- **Database**: PostgreSQL 16 (Alpine) — one isolated instance per service, no shared tables

### Frontend
- Not built yet. The client-facing surface (web/mobile/other apps shown in the architecture) is out of scope for the current phases, which focus on backend services and infrastructure. Will revisit once the core services are working.

### Infra / DevOps
- **API Gateway**: Kong 3.7, DB-less/declarative mode — config in [`kong/kong.yml`](./kong/kong.yml)
- **Local orchestration**: Docker Compose — spins up all 5 databases + Kong + a test upstream with one command

## Project status

### ✅ Phase 1 — Foundation & infra (done)
- [x] Service boundaries defined ([`SERVICES.md`](./SERVICES.md))
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

## Getting started

### Prerequisites
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (with virtualization enabled — Docker won't start without it)

### Run it

```bash
git clone https://github.com/<your-username>/banking-system.git
cd banking-system
docker compose up -d
```

Wait ~10 seconds for the Postgres health checks to pass, then verify:

```bash
docker compose ps
```

All 6 containers should show `running` or `healthy`.

### Verify Postgres

```bash
docker exec -it auth-db psql -U auth_user -d auth_db -c "SELECT 1;"
docker exec -it account-db psql -U account_user -d account_db -c "SELECT 1;"
docker exec -it transaction-db psql -U transaction_user -d transaction_db -c "SELECT 1;"
docker exec -it ledger-db psql -U ledger_user -d ledger_db -c "SELECT 1;"
docker exec -it notification-db psql -U notification_user -d notification_db -c "SELECT 1;"
```

### Verify Kong

```bash
curl -i http://localhost:8000/hello
```

Expected: `hello from behind kong`

### Shut down

```bash
docker compose down
```

## Project layout

```
banking-system/
├── docker-compose.yml       # Postgres x5 + Kong + hello-world test upstream
├── kong/
│   └── kong.yml              # Kong declarative config (DB-less)
├── proto/
│   ├── auth.proto
│   ├── account.proto
│   ├── transaction.proto
│   ├── ledger.proto
│   └── notification.proto
├── SERVICES.md               # Service boundaries & communication rules
└── README.md                 # This file
```

## License

Not yet decided — add a `LICENSE` file before making this public if you intend for others to use or contribute to it.
