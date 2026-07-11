# PingPolicy

Microservices-based API contract monitoring tool. Detects silent schema drift
across distributed microservices by polling live endpoints on a schedule,
diffing the observed JSON shape against a registered contract, caching poll
results in Redis to cut redundant network calls, and firing alerts when drift
is detected.

## Architecture

```
                        ┌─────────────────────────┐
                        │   React Dashboard        │
                        │   (frontend)             │
                        └───────────┬──────────────┘
                                    │ REST
             ┌──────────────────────┼───────────────────────┐
             │                                               │
   ┌─────────▼─────────────┐                      ┌──────────▼──────────────┐
   │ contract-registry-svc  │                      │ polling-alerting-svc     │
   │  - CRUD for contracts  │◄────fetch contract────│  - scheduled polling     │
   │  - Postgres            │        (REST)         │  - Redis response cache  │
   └────────────────────────┘                      │  - JSON diff engine      │
                                                     │  - alerting (webhook/log)│
                                                     │  - Postgres (drift log)  │
                                                     └───────────────────────────┘
```

* **contract-registry-service** – owns the "contract registry": the expected
  JSON schema/shape for each monitored endpoint. Plain CRUD REST API backed
  by PostgreSQL.
* **polling-alerting-service** – the workhorse. A scheduler polls each
  registered target on its configured interval, checks Redis first (cache-aside,
  keyed by target + response hash) to avoid re-fetching/re-diffing an endpoint
  whose last-seen response hash hasn't changed, runs the response through the
  JSON diff engine against the registered contract, persists any detected
  drift, and pushes alerts (webhook + log sink, pluggable).
* **frontend** – React dashboard to register targets/contracts and view drift
  history.

## Redis caching strategy (the "55% fewer redundant calls" bit)

Each poll cycle:
1. Fetch the live endpoint.
2. Hash the response body (SHA-256).
3. Look up `poll:{targetId}:lastHash` in Redis.
4. If the hash is unchanged since the last cycle, skip the (expensive) diff
   entirely — the shape can't have drifted if the payload is byte-identical.
5. If the hash changed (or cache miss), run the full diff engine, persist the
   result, and update the cached hash + a `poll:{targetId}:lastContract`
   snapshot with a TTL.

This turns the diff engine from "run every poll" into "run only when the
payload actually changed," which is where the reduction in redundant work
comes from on endpoints that are mostly stable between polls.

## Running locally

```bash
docker compose up --build
```

* Contract registry: http://localhost:8081
* Polling/alerting service: http://localhost:8082
* Frontend: http://localhost:5173
* Postgres: localhost:5432 (db: pingpolicy / pingpolicy)
* Redis: localhost:6379

## Repo layout

```
pingpolicy/
├── contract-registry-service/   # Spring Boot, contract CRUD, Postgres
├── polling-alerting-service/    # Spring Boot, scheduler, diff engine, Redis, alerts, Postgres
├── frontend/                    # React (Vite) dashboard
└── docker-compose.yml
```
