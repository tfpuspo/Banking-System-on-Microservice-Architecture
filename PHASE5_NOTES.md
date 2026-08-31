# Phase 5 — API Gateway Layer (Kong + GraphQL)

## Item 1: "Route REST/gRPC through Kong" — does not apply as written

This checklist item was written for a generic roadmap where Kong might
proxy gRPC directly to backend services. That's not this project's
architecture. From the very start, the explicit agenda was:

```
Consumers → GraphQL → Kong → gRPC → Microservices
```

Kong only ever routes to **one thing**: `graphql-gateway`. It never talks
gRPC directly to `auth-service` or `account-service` — those only ever
receive gRPC calls from `graphql-gateway` (and each other), never from
Kong or any external client. Configuring Kong to route straight to a gRPC
service would let clients bypass the GraphQL layer entirely, which
contradicts the architecture this whole project has been built around.

**No change made here** — `kong.yml` correctly has exactly one route
(`/graphql` → `graphql-gateway`), which is already correct for this design.

## Item 2: "Add a GraphQL BFF layer" — already done (Phase 2)

`graphql-gateway` **is** this layer. It's been resolving GraphQL
queries/mutations by calling backend services over gRPC since Phase 2 —
nothing new needed here either.

## Item 3: "Wire auth into the gateway" — the real Phase 5 work

Two options were on the table: a Kong JWT plugin, or handling it in the
GraphQL layer itself.

**Decision: GraphQL layer**, via a `WebGraphQlInterceptor`
(`AuthenticationInterceptor.java`) — the Spring for GraphQL equivalent of
Apollo Server's "context function" pattern mentioned in the original plan.

**Why not Kong's JWT plugin**: Kong's JWT plugin expects tokens with an
`iss` claim matching a pre-registered Kong Consumer, and validates against
a secret/key stored in Kong's own config. Our tokens (issued by
`auth-service`, HS384, `sub`/`roles`/`iat`/`exp` claims) don't carry an
`iss` claim, and don't need to — every token comes from exactly one
issuer. Reshaping the token format and provisioning Kong Consumers just to
fit Kong's plugin model would add real complexity for no real benefit at
this scale. The interceptor approach uses the exact same verification
(`auth-service.ValidateToken` over gRPC) we already built and trust.

## How it works

`AuthenticationInterceptor` inspects every incoming GraphQL request's AST
*before* any resolver runs. If the request targets a protected field
(`createAccount`, `myAccounts`, `account`), it requires a valid
`Authorization: Bearer <token>` header and calls `auth-service.ValidateToken`
to verify it — rejecting the request with a clean `UNAUTHORIZED` GraphQL
error immediately if that fails, before any downstream service is ever
touched.

**Public operations, deliberately exempt**: `register`, `login`,
`refreshToken`, `logout`, `validateToken`, `authorize`. These either
establish identity in the first place (can't require a token to log in),
or take their own relevant token as an explicit argument rather than
relying on the header.

## Defense in depth — not redundant

`account-service` still independently calls `auth-service.ValidateToken`
itself (`AccountGrpcService.requireValidUser`), even though the gateway
now checks first. This is intentional: a downstream service should never
blindly trust that an upstream gateway already verified something —
that assumption breaks the moment any other caller (a future internal
service, a test script, a misconfigured route) reaches `account-service`
without going through the gateway. Checking twice costs one extra gRPC
call; skipping the second check risks a real security gap later.

## What actually changed

- **New**: `graphql-gateway/.../interceptor/AuthenticationInterceptor.java`
- **No changes** to Kong, `account-service`, or `auth-service` — this
  phase was scoped entirely to the gateway layer, correctly.
