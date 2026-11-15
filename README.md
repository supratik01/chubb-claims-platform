# Chubb APAC claims platform

## Overview

One web application for motor and property insurance claims across six APAC markets, built for the Chubb APAC
full-stack take-home assessment.

- **Claimants** report an incident against one of their policies, follow the claim's progress, answer questions
  from the claims officer, and see the decision.
- **Claims officers** pick up new claims from a queue, ask for more information, approve or reject, record payment,
  and see the team's workload.
- **Claims managers** can do everything an officer can, and also see the open claims and the outstanding liability
  exposure, per currency, country and line of business.

The backend is one Spring Boot service with strict layers. Anything a person waits for is a REST call; each status
change also publishes a Kafka event, and a consumer keeps the exposure figures up to date. The frontend is an
Angular shell that loads a claimant micro-frontend and a staff micro-frontend at runtime with Native Federation.

Scope was chosen for depth over breadth (see [`docs/requirements.md`](docs/requirements.md)): file uploads,
notifications, market-specific rules and production identity are deliberately left out and explained.

| Document | What it covers |
|---|---|
| [`docs/requirements.md`](docs/requirements.md) | Requirements traced to the brief, the structure file and the slides; must-have or deferred |
| [`docs/architecture.md`](docs/architecture.md) | Diagrams, entities, service boundary, REST and Kafka, micro-frontends |
| [`NOTES.md`](NOTES.md) | Every design decision with alternatives and trade-offs; assumptions; conflicts between the inputs |
| [`docs/walkthrough.md`](docs/walkthrough.md) | Presentation outline, demo script, likely questions, next steps |
| [`.claude/ai-journal.md`](.claude/ai-journal.md) and [`prompts/`](prompts/) | How the AI was directed, and what it corrected along the way |

## Architecture

The diagrams are in [`docs/architecture.md`](docs/architecture.md): the system diagram, the entity relationships and
the claim status workflow.

| Part | Technology | Where |
|---|---|---|
| Claims service | Java 21, Spring Boot 4.1, Spring Data JPA, Spring Security (JWT), Spring Kafka | [`backend/`](backend/) |
| Layers | `api` (controllers, DTOs, errors), `service` (use cases), `domain` (claim aggregate, workflow, ports), `infrastructure` (JPA, Kafka, security, seed data) | [`backend/src/main/java/com/chubb/claims`](backend/src/main/java/com/chubb/claims) |
| Database | PostgreSQL 18, Flyway migrations | [`backend/src/main/resources/db/migration`](backend/src/main/resources/db/migration) |
| Events | Kafka 4.2 topic `claims.claim-events.v1`, transactional outbox, idempotent consumer, dead-letter topic | [`docs/architecture.md`](docs/architecture.md#3-rest-and-kafka) |
| Shell | Angular 22 host: sign-in, navigation, role-based routing | [`frontend/projects/shell`](frontend/projects/shell) |
| Micro-frontends | Claimant area and staff area, loaded at runtime with Native Federation | [`frontend/projects/claimant`](frontend/projects/claimant), [`frontend/projects/staff`](frontend/projects/staff) |
| Shared library | Typed API client, session store, state helpers, theme and components | [`frontend/projects/shared`](frontend/projects/shared) |

## Setup

**Prerequisites:** Docker with Compose v2 (Docker Desktop is fine), about 4 GB of free memory, and ports 4200 and
8080 free. Nothing else is needed to run the application.

**Start everything with one command** from the repository root:

```bash
docker compose up --build
```

This builds the backend and frontend images and starts PostgreSQL, Kafka, the claims service and the web server in
order, each waiting for the one before it to be healthy. From a fresh clone on the development machine (an Apple
Silicon Mac, base images not yet cached locally for this project) it took about two minutes; see
[`docs/walkthrough.md`](docs/walkthrough.md#verification) for the measured figure. Then open
**http://localhost:4200**.

The demo data is seeded on start-up and reset by `docker compose down` followed by `up` (nothing is kept between
runs). To use other ports, set `WEB_PORT` or `BACKEND_PORT`; every setting is listed in [`.env.example`](.env.example).

**Demo users.** All of them use the password `Demo-Passw0rd!` (set by `DEMO_USER_PASSWORD`). The sign-in page lists
them too.

| Username | Role | What to try |
|---|---|---|
| `claimant.mei` | Claimant (Singapore policies) | One claim is waiting for her answer |
| `claimant.jack` | Claimant (Australia policies) | Claims under review, approved and rejected |
| `officer.sam` | Claims officer | Owns the claim waiting on Mei, and an approved claim to settle |
| `officer.priya` | Claims officer | Owns a claim under review |
| `manager.kenji` | Claims manager | Everything an officer sees, plus outstanding exposure |

**Developing without rebuilding images.** Start the back end in Docker and the frontend with live reload:

```bash
docker compose up -d postgres kafka backend
```

```bash
cd frontend && nvm use && npm ci && npm start
```

```bash
cd frontend && npm run start:claimant
```

```bash
cd frontend && npm run start:staff
```

The shell runs on port 4200 and proxies `/api` to the backend on 8080; the micro-frontends run on 4201 and 4202.
Stop the Docker `frontend` service first, because it also uses port 4200. The frontend needs Node 24
(`frontend/.nvmrc`); the backend needs Java 21 and Maven 3.9.

## API Reference

The contract is [`backend/src/main/resources/openapi/claims-api.yaml`](backend/src/main/resources/openapi/claims-api.yaml)
(OpenAPI 3.0). It was written before the controllers, the backend integration tests validate real responses against
it, and the frontend's TypeScript types are generated from it (`npm run generate:api`).

All paths are under `/api/v1`. Send `Authorization: Bearer <accessToken>` from the login response on every other call.
Every error is an RFC 9457 problem document (`application/problem+json`) with a stable `code` and, for validation
failures, a list of field errors.

| Method and path | Who | Purpose |
|---|---|---|
| `POST /auth/login` | Anyone | Exchange username and password for a one-hour token |
| `GET /policies` | Claimant | The claimant's own policies |
| `POST /claims` | Claimant | Report an incident (creates a claim in `SUBMITTED`) |
| `GET /claims` | Everyone | Claimants get their own claims; staff filter by `assignment` (`UNASSIGNED`, `MINE`, `ALL`) and `status` |
| `GET /claims/{claimId}` | Owner or staff | One claim with its status history and information requests |
| `POST /claims/{claimId}/assignment` | Staff | Pick up a new claim (`SUBMITTED` to `UNDER_REVIEW`) |
| `POST /claims/{claimId}/information-requests` | Assigned officer or manager | Ask the claimant a question (`UNDER_REVIEW` to `INFO_REQUESTED`) |
| `POST /claims/{claimId}/information-requests/{requestId}/response` | Owning claimant | Answer (`INFO_REQUESTED` to `UNDER_REVIEW`) |
| `POST /claims/{claimId}/decision` | Assigned officer or manager | Approve with an amount, or reject with a reason |
| `POST /claims/{claimId}/settlement` | Assigned officer or manager | Record payment (`APPROVED` to `SETTLED`) |
| `GET /dashboard/workload` | Staff | Claims per officer by whose move it is, plus the unassigned backlog |
| `GET /dashboard/exposure` | Manager | Open claims and outstanding amount per currency, country and line of business |

Through the web server the base address is `http://localhost:4200/api/v1`; the service itself is on
`http://localhost:8080/api/v1`. A Postman collection that walks the whole demo flow with assertions is in
[`postman/`](postman/claims-api.postman_collection.json):

```bash
postman collection run postman/claims-api.postman_collection.json
```

## Testing

| Suite | Count | What it covers | Needs |
|---|---|---|---|
| Backend unit tests | 58 | Status transitions, the claim aggregate's rules (amounts, dates, history, events), each service's access and business rules, the exposure projection's idempotency, and an ArchUnit test that enforces the layering | Java 21 |
| Backend integration tests | 20 | The main endpoints over HTTP on real PostgreSQL and Kafka (Testcontainers), every response validated against the OpenAPI file; authentication and role rules; the event flow from outbox to Kafka to the exposure summary, including redelivery and the dead-letter topic | Docker |
| Frontend unit and component tests | 80 | Session store, interceptor and guards; request-state helpers; shared components; the sign-in, report-incident, my-claims, claim, work-queue and exposure screens; the staff action rules and decision form | Node 24 |
| End-to-end tests | 7 | The full lifecycle with three people signed in at once (report, pick up, question, answer, approve, decision seen, payment, exposure moving at each step); rejection with a reason; access by role; sign-in errors and return to the requested page | The running stack, Node 24 |
| Postman collection | 22 requests, 43 assertions | The demo flow and the error responses, through the public API | The running stack, Postman CLI |

Run them from the repository root:

```bash
cd backend && mvn test
```

```bash
cd frontend && npm ci && npm test
```

```bash
docker compose up -d --build --wait && cd frontend && npx playwright install chromium && npm run e2e
```

Tests are named by behaviour, for example `a claim goes from report to approval, and the claimant and manager see
each step`. The end-to-end tests run one at a time because the exposure test measures a change in a shared total.
