# Phase 6 — Expand Domain Services

## The 3 checklist items

**"Add `account-service`, `transaction-service`, `ledger-service`, each with
its own Postgres DB, gRPC contracts"** — done. `account-service` existed
since Phase 4; `transaction-service` and `ledger-service` are new this
phase, each with its own database (`transaction_db`, `ledger_db` — both
provisioned back in Phase 1, sitting empty until now) and its own `.proto`
contract (also written in Phase 1, unused until now).

**"...and Kong route"** — doesn't apply, same reasoning as Phase 5. Kong
only ever routes to `graphql-gateway`; `transaction-service` and
`ledger-service` are purely internal, reached only via gRPC from
`graphql-gateway` (and from each other, via Kafka) — never directly by a
client. No Kong config changed this phase.

**"Handle cross-service transactions carefully... Saga pattern or outbox
pattern rather than naive multi-service calls"** — this is the real work.
Below is exactly how it's implemented, using Kafka as the async transport
per your instruction.

## Why not a naive multi-service call

The "naive" approach the plan warns against would look like: `transaction-
service.InitiateTransfer` directly, synchronously calls
`ledger-service.PostEntry` over gRPC, waits for the response, then returns.
The problem: what if `transaction-service` writes its own transaction row,
then crashes (or the network drops) *before* the gRPC call to
`ledger-service` completes? Now a transaction exists that will never
resolve — no error, no success, just silently stuck. Worse: what if the
gRPC call to `ledger-service` succeeds but `transaction-service` crashes
before recording that success? Now money moved but the system doesn't know
it. Distributed systems don't get atomic all-or-nothing guarantees across
two separate databases for free — that's exactly the problem Sagas and the
Outbox pattern exist to solve.

## The actual flow, end to end

```
1. graphql-gateway calls transaction-service.InitiateTransfer (gRPC, sync)
       ↓
2. transaction-service, in ONE local DB transaction:
   - writes a `transactions` row (status = PENDING)
   - writes an `outbox_events` row ("TransferRequested")
   Both succeed together or neither does — no partial state possible.
   Returns PENDING to the caller immediately. Money has NOT moved yet.
       ↓
3. OutboxPublisher (background poll, every 500ms) reads the unpublished
   outbox row, sends it to Kafka topic `ledger.commands`, marks it published.
   If Kafka is briefly down, the row just waits — nothing is lost.
       ↓
4. ledger-service consumes the event. In ONE local DB transaction:
   - checks the real balance (SUM of that account's journal entries)
   - if sufficient: posts TWO journal rows (debit + credit) AND writes its
     own outbox row ("LedgerEntryPosted")
   - if insufficient: writes an outbox row ("LedgerEntryFailed") instead,
     posts no journal entries at all
       ↓
5. ledger-service's own OutboxPublisher relays that result to Kafka topic
   `ledger.results`
       ↓
6. transaction-service consumes it, updates its `transactions` row to
   COMPLETED or FAILED
       ↓
7. Client polls `transaction(transactionId)` via GraphQL to see the final status
```

This is a **choreographed Saga**: each service reacts to events, no central
orchestrator, no distributed transaction coordinator. Combined with the
**Outbox pattern** at both write points (step 2 and step 4), the guarantee
holds: an event is never silently lost between "I did the work" and
"I told everyone about it," because both happen in the same local
transaction, atomically, at each service.

## Idempotency — required, not optional, with Kafka

Kafka guarantees **at-least-once** delivery — a consumer *will*
occasionally see the same message twice (e.g. if it crashes after
processing but before committing its offset). Two protections exist:

- `ledger-service`: a **unique constraint** on `(idempotency_key,
  account_id)` makes a duplicate journal insert fail at the database level,
  checked explicitly before attempting the insert — a replayed event is a
  safe no-op.
- `transaction-service`: `LedgerResultConsumer` only acts if the
  transaction is still `PENDING` — a duplicate result event for an
  already-`COMPLETED` transaction is ignored.

## Why polling for the outbox relay, not CDC

`OutboxPublisher` polls the database every 500ms rather than using
Change Data Capture (e.g. Debezium reading Postgres's write-ahead log).
Polling is simpler to run and reason about; CDC is lower-latency and
lower-load at real scale, but adds real infrastructure (a Debezium
connector, Kafka Connect) disproportionate to this system's current size.
Worth revisiting if outbox latency (currently: up to 500ms before an event
is even sent) ever becomes a real problem.

## Known simplifications, stated honestly

- **No check that `fromAccountId` actually belongs to the caller.**
  `account-service` enforces this for direct reads (`GetAccount`), but
  `transaction-service` doesn't yet call `account-service` to verify
  ownership before initiating a transfer. A real system would need this —
  flagged here rather than silently skipped.
- **No balance check happens synchronously.** The client gets back
  `PENDING` immediately regardless of whether the source account can
  actually cover the transfer — insufficient funds is only discovered
  asynchronously, in `ledger-service`, and only visible by polling
  `transaction()` afterward. A production UI would want to poll or
  subscribe for the resolved status, not assume success from the initial response.
- **No dead-letter handling.** If `ledger-service`'s consumer repeatedly
  fails to process an event (a bug, a malformed payload), it currently just
  logs and moves on — no retry-with-backoff, no dead-letter topic to
  quarantine poison messages for manual inspection. Real production Kafka
  consumers need this.

## Testing it

```graphql
mutation {
  transfer(fromAccountId: "...", toAccountId: "...", amountMinorUnits: 500, currency: "USD") {
    transactionId
    status
  }
}
```
Expect `status: "PENDING"` immediately. Then, a moment later:
```graphql
query {
  transaction(transactionId: "PASTE_ID_HERE") {
    status
  }
}
```
Expect `status: "COMPLETED"` (if the source account had sufficient balance)
or `"FAILED"` otherwise — both accounts need to actually exist
(`createAccount` first) for the IDs to resolve to anything meaningful.
