# auth-service

Java 17 + Spring Boot + gRPC implementation of `auth.proto`. Handles
registration, login, token refresh, token validation, basic authorization,
and logout.

## What it does on startup
1. Connects to `auth-db` (Postgres) via Spring Data JPA
2. **Creates/updates its own tables automatically** (`spring.jpa.hibernate.ddl-auto=update`)
   — no manual SQL needed
3. Starts a gRPC server on port `50051` (via `grpc-server-spring-boot-starter`),
   with reflection enabled so `grpcurl` can talk to it without needing the
   `.proto` file

Note: this service has no `spring-boot-starter-web` dependency, so no
embedded Tomcat starts — this process is a pure gRPC server, nothing listens
on an HTTP port.

## Architecture within the service

| Package | Responsibility |
|---|---|
| `entity/` | JPA entities — `UserEntity`, `SessionEntity` |
| `repository/` | Spring Data JPA repositories (interfaces only, Spring generates the implementation) |
| `grpc/AuthGrpcService` | The actual gRPC service — implements every RPC from `auth.proto`, calls the repositories |
| `util/JwtUtil` | Issues and verifies JWT access tokens |

## Run it (as part of the whole system)

From the `banking-system/` root folder:

```bash
docker compose up -d --build
```

Check it started cleanly:

```bash
docker compose logs auth-service
```

You should see Spring Boot's startup banner, followed by something like:
```
... o.l.g.s.b.a.GrpcServerFactoryAutoConfiguration : gRPC Server started, listening on address: *, port: 50051
```

## Test it with grpcurl

[grpcurl](https://github.com/fullstorydev/grpcurl) is like `curl`, but for gRPC.

```bash
grpcurl -plaintext localhost:50051 list
# Expected: auth.AuthService

grpcurl -plaintext -d '{"email":"[email protected]","password":"hunter2","full_name":"Test User"}' \
  localhost:50051 auth.AuthService/Register

grpcurl -plaintext -d '{"email":"[email protected]","password":"hunter2"}' \
  localhost:50051 auth.AuthService/Login
```

See `graphql-gateway/README.md` for testing the same flow through GraphQL +
Kong instead — that's the real intended path for this project.

## Known simplifications (intentional, for now)

- **JWT uses HS256 with a shared secret**, set via `JWT_SECRET` in
  `docker-compose.yml`. Fine for local dev. Before any real deployment, this
  should move to RS256 (a private/public key pair).
- **Authorize is minimal** — real per-resource, per-action rules are Phase 3 work.
- **Roles are stored as a flat string set** (`user_roles` table with just a
  `role_name` column), not a full `roles` catalog table with a foreign key —
  simpler while the role list stays small and fixed.
- The JWT secret and DB passwords are committed in plain text in
  `docker-compose.yml` — fine for local dev, not for anything real.

## Next (Phase 3 / Phase 4)

- Expand `Authorize` with real resource/action rules
- Stand up a second service (e.g. `account-service`) that calls
  `auth-service.ValidateToken` over gRPC
