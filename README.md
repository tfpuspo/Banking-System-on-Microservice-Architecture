# Banking System — Microservices Architecture

A banking system built on GraphQL, gRPC, and Kong API Gateway — microservices with one isolated Postgres database each, safe money transfers via a Kafka Outbox flow, and identity re-verified at every service instead of trusted from the caller.

## Tech stack

- **Backend**: Java 17, Spring Boot 3.2 (one service per module)
- **Service-to-service**: gRPC — contracts in [`proto/`](./proto)
- **API layer**: GraphQL (`graphql-gateway`) — the only interface clients see
- **API Gateway**: Kong 3.7, declarative config in [`kong/kong.yml`](./kong/kong.yml)
- **Async messaging**: Apache Kafka — Outbox pattern for transfers
- **Database**: PostgreSQL 16, one instance per service
- **Frontend**: Next.js 14, TypeScript, Tailwind CSS
- **Orchestration**: Docker Compose

Services: `auth-service`, `account-service`, `transaction-service`, `ledger-service`, `notification-service`.

## Run it

```bash
git clone https://github.com/<your-username>/banking-system.git
cd banking-system
docker compose up -d
```

- Frontend: `http://localhost:3000`
- GraphQL through Kong: `http://localhost:8000/graphql`

![Banking System Architecture](<images>/banking_system_architecture_v2.png)

<img src="images/img-1.png">
<img src="images/img-2.png">
<img src="images/img-3.jpeg">
<img src="images/img-4.jpeg">


