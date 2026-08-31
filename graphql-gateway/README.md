# graphql-gateway

Java 17 + Spring Boot + Spring for GraphQL. Sits behind Kong, translates
GraphQL queries/mutations into gRPC calls to `auth-service`. This is the
"BFF" (Backend-For-Frontend) layer completing the path:

```
Consumers → GraphQL → Kong → gRPC → auth-service
```

## How it works
- `src/main/resources/graphql/schema.graphqls` defines the schema — Spring
  for GraphQL auto-discovers any `.graphqls` file under `resources/graphql/`
- `AuthGraphQlController` has one `@QueryMapping`/`@MutationMapping` method
  per schema field — each one builds a gRPC request, calls `auth-service`
  via an injected `@GrpcClient` stub, and maps the response into a small DTO
  record
- DTO records exist so GraphQL field names line up cleanly with the
  generated protobuf getters (protobuf names repeated fields like
  `getRolesList()`, which doesn't match GraphQL's `roles` field automatically)
- `GrpcExceptionResolver` converts gRPC errors (e.g. `ALREADY_EXISTS` on a
  duplicate registration) into readable GraphQL errors instead of a generic
  500

## Full request path, end to end

```
Browser/Postman → http://localhost:8000/graphql   (Kong)
                        ↓
              graphql-gateway:4000                 (Spring for GraphQL)
                        ↓  gRPC
                 auth-service:50051                 (Spring Boot + gRPC)
                        ↓
                    auth-db:5432                     (Postgres)
```

## Example queries

Through Kong (the real path): `POST http://localhost:8000/graphql`
Directly against the gateway (bypasses Kong, useful for isolating bugs):
`POST http://localhost:4000/graphql`

A browser-based GraphQL playground (GraphiQL) is also available at
`http://localhost:4000/graphiql` for manual testing without any extra tools.

### Register
```graphql
mutation {
  register(email: "[email protected]", password: "hunter2", fullName: "Test User") {
    userId
    email
  }
}
```

### Login
```graphql
mutation {
  login(email: "[email protected]", password: "hunter2") {
    accessToken
    refreshToken
    expiresIn
  }
}
```

### Validate a token
```graphql
query {
  validateToken(accessToken: "PASTE_ACCESS_TOKEN_HERE") {
    valid
    userId
    roles
  }
}
```

### Refresh
```graphql
mutation {
  refreshToken(refreshToken: "PASTE_REFRESH_TOKEN_HERE") {
    accessToken
    refreshToken
    expiresIn
  }
}
```

### Logout
```graphql
mutation {
  logout(refreshToken: "PASTE_REFRESH_TOKEN_HERE") {
    success
  }
}
```

## Run it

From the `banking-system/` root:

```bash
docker compose up -d --build
```

`--build` matters — Compose needs to actually build both `auth-service` and
`graphql-gateway` from their Dockerfiles (Maven compiles each one), not pull
pre-made images.

## A note on version compatibility

This uses Spring Boot 3.2.5, gRPC 1.62.2, and the `net.devh`
grpc-spring-boot-starter 3.1.0.RELEASE bridge library that connects them.
These three projects are maintained separately, so if the first build fails
with a dependency resolution error, it's most likely a version mismatch
between them — check
[net.devh's compatibility matrix](https://github.com/grpc-ecosystem/grpc-spring/blob/master/docs/COMPATIBILITY.md)
and adjust `grpc.version` / `grpc-spring-boot.version` in `pom.xml`
accordingly. This is the most likely source of a first-run error in this
codebase.

## Known simplifications (intentional, for now)

- No authentication on the GraphQL layer itself yet — anyone who can reach
  `/graphql` can call `register`/`login`. JWT enforcement at the Kong layer
  for *other* protected routes comes later, once there's something to protect.
- `graphql-gateway` only knows about `auth-service` so far. As
  `account-service`, `transaction-service`, etc. get built, this gateway's
  schema and controller grow to call them too — it becomes the single entry
  point for everything.

## Next (Phase 3 / Phase 4)

- Expand `Authorize` with real resource/action rules
- Stand up a second service (e.g. `account-service`) that calls
  `auth-service.ValidateToken` over gRPC — proving service-to-service
  communication works
