# Service Boundaries

| Service | Responsibility | Owns Data |
|---|---|---|
| `auth-service` | Registration, login, JWT issuance/refresh, role & permission checks | `users`, `roles`, `sessions` |
| `account-service` | Bank accounts (checking/savings), balances, account status | `accounts`, `account_holders` |
| `transaction-service` | Initiating transfers, deposits, withdrawals (orchestrates, doesn't hold money truth) | `transactions`, `transaction_status` |
| `ledger-service` | Source of truth for money movement (double-entry ledger), balance reconciliation | `ledger_entries`, `journal` |
| `notification-service` | Emails/SMS/push for account activity, alerts | `notification_log`, `preferences` |

## Communication rules
- Every service gets **its own Postgres database** — no service reaches into another's tables directly.
- Services talk to each other **only via gRPC** (defined in `/proto`).
- Clients (web/mobile) talk **only to the GraphQL layer**, which sits behind Kong and fans out to services over gRPC.
- `transaction-service` never mutates balances directly — it calls `ledger-service`, which is the only service allowed to write ledger entries. This keeps money movement auditable and single-sourced.
- `auth-service` issues JWTs (RS256). Other services **verify tokens locally** using the shared public key (no round-trip per request) and call `auth-service.Authorize` only for higher-risk actions.

## Build order (matches implementation phases)
1. `auth-service` — prove authentication + authorization first
2. `account-service` — prove service-to-service gRPC (calls auth-service)
3. Kong + GraphQL gateway — prove the client-facing path
4. `transaction-service`, `ledger-service`, `notification-service` — expand domain
