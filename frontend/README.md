# frontend — "The Ledger"

Next.js 14 (App Router) + TypeScript + Tailwind CSS. The web client for the
banking system — talks to `graphql-gateway` through Kong, exactly the same
path GraphiQL uses for manual testing.

```
Browser → http://localhost:8000/graphql (Kong) → graphql-gateway → gRPC → auth-service
```

## Pages

| Route | What it does |
|---|---|
| `/` | Landing page |
| `/register` | Calls the `register` mutation |
| `/login` | Calls the `login` mutation, saves the token pair to `localStorage` |
| `/dashboard` | Calls `validateToken` on load (proves the token actually works end-to-end), shows a verification "stamp," and calls `logout` |

## Design

The visual direction ties directly to what the backend actually does — this
is a system built around a literal ledger (`ledger-service`, double-entry
journal entries), so the UI borrows from that: a deep ink-navy and brass
palette, a serif display face for headings, monospace for anything
numeric/token-like (account IDs, session data), and ruled-paper hairlines
structuring each card. The dashboard's "stamp" is the one bold, memorable
element — it visually confirms your session was verified by `auth-service`,
the same way a bank stamp confirms an entry in a passbook.

## Run it

From the `banking-system/` root:

```bash
docker compose up -d --build
```

Then open `http://localhost:3000`.

## Important: environment variables and Docker build time

`NEXT_PUBLIC_GRAPHQL_URL` is baked into the JavaScript bundle **at build
time**, not read at container startup — this is a Next.js constraint, not a
choice made here. That's why `docker-compose.yml` passes it as a `build.args`
entry, not a plain `environment:` entry (that wouldn't work — the browser
bundle would already be built without it).

It's also deliberately set to `http://localhost:8000` (not an internal
Docker service name like `kong:8000`) — the browser makes these requests
directly from the user's machine, so it needs an address the browser can
actually reach, not one that only makes sense inside the Docker network.

## Local development (outside Docker)

```bash
cd frontend
npm install
npm run dev
```

Create a `.env.local` file if you want to point at a different backend URL:
```
NEXT_PUBLIC_GRAPHQL_URL=http://localhost:8000/graphql
```

## Known simplifications (intentional, for now)

- **Tokens live in `localStorage`**, which is simple but vulnerable to XSS.
  A more production-appropriate approach is an httpOnly cookie set by a
  backend endpoint the browser's JS can't read directly at all.
- **No token refresh flow wired up yet** — when the 15-minute access token
  expires, the dashboard will just show "session not valid" rather than
  silently refreshing. Calling `refreshToken` automatically before that
  happens is a natural next improvement.
- **No form validation beyond browser defaults** (`required`, `minLength`)
  — enough for now, not hardened.
