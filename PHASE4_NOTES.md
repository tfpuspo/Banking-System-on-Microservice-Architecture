# Phase 4 — gRPC Between Microservices

## What this proves

`account-service` never trusts a client-supplied `user_id`. Every operation
calls `auth-service.ValidateToken` over real gRPC first, and derives the
true caller identity from that response. This is the actual inter-service
trust chain a microservices system needs — not just "two services can ping
each other," but "a service correctly delegates identity verification to
the service that owns it."

## The propagation chain

```
Browser
  │  HTTP header: Authorization: Bearer <token>
  ▼
graphql-gateway
  │  TokenPropagationFilter reads the header into RequestTokenHolder (ThreadLocal)
  │  AuthPropagationClientInterceptor attaches it as gRPC metadata on every outgoing call
  ▼
account-service
  │  AuthPropagationServerInterceptor reads the metadata into gRPC Context
  │  AccountGrpcService.requireValidUser() reads it from Context
  │  AuthForwardingClientInterceptor re-attaches it when calling onward
  ▼
auth-service
  │  ValidateToken verifies the JWT signature, returns userId + roles
  ▼
account-service uses the VERIFIED identity, never the client's claim
```

## Interceptors added (Phase 4, step 11)

Every service now has, consistently:
- **LoggingServerInterceptor** — logs every call: method, status, duration
- **ExceptionTranslatingServerInterceptor** — catches unhandled exceptions
  and returns a clean `Status.INTERNAL` with a real message, instead of
  gRPC's unhelpful generic "Application error processing RPC" (the exact
  bug we hit and had to debug during Phase 2's Register flow)

`account-service` and `graphql-gateway` additionally have the
propagation-specific interceptors described above.

## Authorization model update

Phase 3's `Authorize` originally **failed closed** on `account` resources,
since no service existed yet to supply real ownership data — that was the
honest, correct behavior at the time.

Now that `account-service` exists, the division of responsibility is:
- **`auth-service.Authorize`** confirms role-level *capability* — "can a
  customer even perform `read` on `account` resources, in principle?"
- **`account-service`** confirms instance-level *ownership* itself, using
  its own data — "is this *specific* account actually this user's?"

This matches how real systems typically split authorization: a central
identity service knows about roles and general permissions; individual
services own the ownership checks for the resources only they have data
for. `auth-service` could never correctly answer "is account #1234 yours,"
since it has no accounts table — asking it to fake that check would be
worse than not checking at all.

## Service discovery (step 12)

Right now: **static addresses via Docker Compose's built-in DNS.**
`account-service:50051` resolves correctly because Compose gives every
service a hostname matching its name, on the shared `banking-net` network.
This is genuinely fine for local development and even small production
deployments — no extra infrastructure needed.

**What would change for a larger production deployment:**
- **Consul** — a dedicated service registry; services register themselves
  on startup, callers look up current addresses dynamically. Useful once
  services scale to multiple instances or move between hosts.
- **Kubernetes DNS** — if deployed on K8s, this problem mostly disappears;
  K8s Services already provide the same kind of stable DNS name
  (`account-service.default.svc.cluster.local`) Compose gives us today,
  just with real load-balancing and health-aware routing across pods.
- **Kong's upstream/service objects** — Kong itself can maintain a pool of
  backend targets per service with health checks, rather than a single
  static URL — relevant once any service runs as more than one replica.

None of this is needed at the current scale — noting it here so the
tradeoff is visible when it becomes relevant, not because it's missing now.

## Testing account-service

```graphql
mutation {
  createAccount(type: "CHECKING", currency: "USD") {
    accountId
    userId
    type
    status
    balanceMinorUnits
  }
}
```
Requires a real `Authorization: Bearer <access_token>` header on the HTTP
request (Postman: Authorization tab → Bearer Token → paste your token).
Without it, expect `UNAUTHENTICATED`.

```graphql
query {
  myAccounts {
    accountId
    type
    balanceMinorUnits
  }
}
```
Returns only accounts belonging to whoever the token belongs to — try it
with two different users' tokens to see the lists differ.
