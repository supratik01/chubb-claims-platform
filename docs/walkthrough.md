# Walkthrough

For the reviewer, and for the 30 to 60 minute interview. Decisions are referred to by their number in
[`NOTES.md`](../NOTES.md) (D1 to D24); requirements by their ID in [`requirements.md`](requirements.md).

## 1. Presentation outline (15 to 20 minutes)

| Time | Segment | What to show | Points to make |
|---|---|---|---|
| 0:00 | The problem and the scope I chose | `docs/requirements.md` | Three people with three problems: claimants hear nothing, staff have no single queue, managers cannot see exposure. Depth on one lifecycle over breadth (D1). Deferred items each have a reason; most come from the brief's own "leave out" list. |
| 2:00 | Backend architecture | The diagram in `docs/architecture.md` | One service with strict layers (D2, D3); the ArchUnit test enforces them. Workflow rules live on the claim aggregate (D4). REST when someone waits, Kafka for consequences (D5). |
| 5:00 | Events and exposure | The event schema and the outbox | Transactional outbox, so no event is lost or invented (D6). Each event carries the old and new outstanding amount; the consumer applies the difference idempotently (D7). Exposure is the sum of reserves (D8). |
| 8:00 | Frontend architecture | `frontend/projects`, `federation.config.mjs` | A shell plus two micro-frontends split by user type, loaded at runtime with Native Federation (D13, D14). `canMatch` checks the role before the code is downloaded. The shared library is a singleton: session, API client, components, theme. One small store per screen (D15, D24). Types generated from the contract (D11). |
| 10:00 | Demo | Section 2 below | The full lifecycle with two users at once, then the manager's view. |
| 16:00 | Quality and how I worked with the AI | `.claude/ai-journal.md`, test reports | The test pyramid (section 4). Defects the tests caught in AI-written code, and where the plan was corrected: journal entries 03 to 08. |
| 19:00 | What I would do next | Section 5 below | The top three, in order. |

## 2. Demo script

Start the stack with `docker compose up --build` before the interview and open http://localhost:4200. Every demo
user's password is `Demo-Passw0rd!`. The session is stored per browser tab (D20), so two tabs can hold two different
users: sign in as the claimant in one tab, then open a second tab by typing the address (a tab opened from a link
copies the first tab's session) and sign in as the officer.

1. **Claimant: the problem solved.** Sign in as `claimant.mei`. "My claims" shows every claim with its status, and a
   banner says one claim needs her answer. Open it: the next step is stated in plain words, and the timeline shows
   each change, who made it and when.
2. **Claimant: report an incident.** Choose *Report an incident*. Submit the empty form to show the validation
   messages: focus moves to the first invalid field, and each message is linked to its field, so a screen reader
   reads it out. Choose the motor policy, enter yesterday's date,
   a description and SGD 1,000. Submit: the new claim opens in *Submitted*.
3. **Officer: the queue.** In the second tab, sign in as `officer.sam`. The *Unassigned* tab lists new claims oldest
   first. Pick up the new claim: it opens in *Under review*, assigned to Sam. Point out that only the actions the
   workflow allows are offered.
4. **Officer: ask a question.** Send "Do you have a photo of the damage?". The claim moves to *Information
   requested* and the action panel now says it is waiting on the claimant.
5. **The asynchronous part.** Switch to the claimant's tab without reloading. Within ten seconds the claim shows
   *Action required* and the question (D16). Answer it.
6. **Officer: decide.** Back in the officer's tab the answer is visible. Approve SGD 800 with a note. Try 90,000
   first to show the sum-insured check.
7. **Claimant: the decision.** The claimant's tab shows *Decision: approved*, the amount and the note.
8. **Manager: workload and exposure.** Sign out and sign in as `manager.kenji` (or use a third tab). *Team workload*
   shows what each officer holds and whose move it is. *Exposure* shows the outstanding amount per currency and per
   country and line of business. Explain that the figure moved from +1,000 to +800 when the claim was approved,
   because the reserve became the approved amount, and that it was updated by the Kafka consumer, not by the request.
9. **Close the loop.** As the officer, record the payment. The manager's exposure for SGD drops by 800.
10. **If there is time:** show `GET /dashboard/exposure` as an officer in Postman (403 problem document), and the
    ArchUnit test failing if a controller imports a repository.

The same flow is automated in `frontend/e2e/claim-lifecycle.spec.ts` and in the Postman collection, so it can be
shown running if the live demo misbehaves.

## 3. Likely "why not X?" questions

**Why not microservices? The role profile lists them.** One team, one aggregate, one database transaction per
command. Splitting now adds distributed transactions and operational cost for no benefit (D2). The seams are there:
the exposure projection only talks to the rest through Kafka and can move out with one listener, one service class
and two tables.

**Why not send to Kafka directly, or after commit with a transactional event listener?** Both can lose an event
(crash between commit and send) or publish one for a transaction that rolled back. The outbox writes the event in
the same transaction as the claim, and a relay publishes it (D6). The cost is up to half a second of latency and
at-least-once delivery, which the consumer handles (D7).

**Why build an exposure read model? `SELECT SUM(reserve_amount)` would do.** At this data size it would, and I say
so in D7. The brief explicitly suggests a consumer maintaining a summary, and the read model is what lets reporting
become its own service later. The trade-off is drift if the consumer has a bug; the remedy is a rebuild from the
topic, which is a next step.

**What happens if Kafka is down?** Claims are still accepted, because the request only writes to PostgreSQL. Events
wait in the outbox and are published when Kafka returns; the exposure figures lag until then. Nothing is lost.

**What if the same event arrives twice?** The consumer records each event id in `processed_event` in the same
transaction as the summary update, and skips repeats. An integration test delivers an event twice (D7).

**Why a home-made login instead of Keycloak or OAuth?** The brief says real authentication can be left out. A
header-based role switch would not be role-based access, so the API issues a standard signed JWT and Spring
Security validates it (D9). Moving to an identity provider changes configuration, not controllers.

**Why keep the token in sessionStorage and not an httpOnly cookie?** It survives a reload and dies with the tab. A
script could read it if there were an XSS flaw; Angular's escaping limits that. The cookie is better and is the first
hardening step, but needs CSRF protection and a backend change (D20).

**The brief says one Angular app. Why micro-frontends?** The master prompt and the rubric require them, and I
recorded the conflict. The user still sees one app: one address, one sign-in. Internally it is split along the one
boundary that matters, the user type, so a claimant's browser never downloads staff code.

**Why Native Federation and not Module Federation?** Angular's default builder is esbuild; Module Federation needs
webpack. Native Federation uses import maps and ES modules and keeps the default builder (D13).

**Why not NgRx?** Six screens, each owning server data for its own lifetime. NgRx would add more code than the
features. Two small helpers (`QueryState`, `CommandState`) give every store the same predictable shape, with tests
(D15, D24).

**Why polling and not WebSockets or server-sent events?** Push needs connection management and per-user fan-out
from Kafka. Ten-second refresh shows the same thing for this scope (D16). It is on the next-steps list for scale.

**Why not generate the controllers from the OpenAPI file, or the file from the controllers?** Hand-written
controllers read better; generated server code is awkward. Drift is caught by validating every integration-test
response against the file. The frontend's types are generated from it (D11).

**Why optimistic locking?** Two officers picking up the same claim is rare and human-speed; the loser gets a clear
409 and the screen reloads (D12). Pessimistic locks would hold rows across a user's think time or need extra care.

**Why are there JPA annotations in the domain layer?** A deliberate, small leak to avoid a second model and mappers
(D3). The ArchUnit rules still stop the domain from knowing about web, messaging or security.

**Why do the end-to-end tests need Docker, and why run serially?** They prove the full path from a click to a
Kafka-built figure on another user's screen, which a mocked backend cannot. Serial runs keep the exposure deltas
exact (D23).

**How did you use the AI, and where did you overrule it?** Every prompt is in `prompts/`, and the journal records
what was produced, the alternatives weighed and every correction. Examples: the event's amount fields, the reserve
as the exposure basis, the "today" check across time zones, an infinite-reload risk in the stores, a form that would
have reloaded the page, and a keyboard focus bug found by an accessibility test.

## 4. What is tested

| Layer | Count | Examples |
|---|---|---|
| Backend unit | 58 | Every status transition; the aggregate's amount, date and history rules; each service's access rules; the projection's idempotency; four ArchUnit layering rules |
| Backend integration | 20 | Real PostgreSQL and Kafka through Testcontainers; responses validated against the OpenAPI file; role and token checks; ordered, keyed events; redelivery; the dead-letter topic |
| Frontend unit and component | 83 | Session, interceptor, guards, state helpers, shared components, focus management, every screen's key behaviour |
| End-to-end | 14 | The lifecycle with three users; rejection; access by role; axe-core scans of every main screen in both themes; keyboard use |
| Postman | 22 requests, 43 assertions | The demo flow and the error responses |

## 5. Next steps, in priority order

1. **Purge old outbox rows and processed-event ids.** Found while preparing this walkthrough: published
   `outbox_event` rows and `processed_event` ids are never deleted, so both tables grow without bound. A scheduled
   job that deletes published rows older than a retention period, and ids older than the consumer's redelivery window,
   would fix it. Not done because it was not required for the requested behaviour.
2. **Harden the session.** An httpOnly, SameSite cookie with CSRF protection instead of a script-readable token,
   refresh tokens, and an identity provider in place of the demo login (D9, D20; B17).
3. **Test a real concurrent update.** The 409 for an optimistic-lock collision is mapped and the UI handles it (a
   component test simulates it), but no backend test forces two commits to race.
4. **Paging in the lists.** The claim lists ask for up to 100 claims and have no paging controls; the API already
   pages. A claimant or queue with more than 100 claims would show only the first 100.
5. **Notifications.** A new consumer group on `claims.claim-events.v1` that emails or texts the claimant (B15).
6. **Server push.** Server-sent events fed from Kafka instead of ten-second polling, once user numbers make the
   polling cost matter (F11).
7. **Rebuild and reconcile the exposure read model.** A replay tool and a scheduled comparison with
   `SUM(reserve_amount)` to detect drift (D7).
8. **A continuous-integration pipeline** that runs all five suites and builds and scans the images (B18).
9. **Retry a failed micro-frontend without a full reload.** The router caches a failed remote load, so the fallback
   page's "Try again" reloads the whole page.
10. **Document uploads** with object storage, size limits and virus scanning (B14).
11. **Market rules, translations and locale formats** for the six countries (B16, F12).
12. **Time-to-decision metrics** in the workload view (assumption 6).

## Verification

Everything below was run on 15 November 2026 on the development machine (Apple Silicon Mac, Docker 29.4), from a
fresh clone of the final commit, not from the working copy.

**Start-up time.** `docker compose build --no-cache` followed by `docker compose up -d --wait` took **80 seconds** to
all four services healthy (61 seconds building, 19 seconds starting). The JRE and Kafka images were removed first
and re-pulled during that run. Two things were still warm and could not be cleared without deleting Docker build
cache shared with other projects on the machine: the Maven, Node and nginx base images, and the Maven and npm
download caches. Measured separately in clean containers, a cold Maven dependency download takes 59 seconds and a
cold `npm ci` 13 seconds; the two images build in parallel, so the backend is the critical path. A machine with
nothing cached should therefore need roughly two and a half to three and a half minutes, depending on bandwidth for
about half a gigabyte of base images. That figure is an estimate, not a measurement.

**Test results from the fresh clone.**

| Suite | Result |
|---|---|
| `mvn test` (backend) | 78 run, 0 failures, 0 errors, 0 skipped (25 seconds) |
| `npm test` (shared, shell, claimant, staff) | 83 passed, 0 failed |
| `npm run e2e` (Playwright against the running stack) | 14 passed, 0 failed |
| `postman collection run` | 22 requests, 43 assertions, 0 failed |

**Repository hygiene.** No build output, dependencies, logs, OS files or `.env` files are tracked. The only
credential-like strings are the documented local defaults (database password, JWT signing key, demo password), all
overridable through environment variables. No commit message is a bare "update" or "fix".

### Structure requirements R1 to R17

| # | Requirement | Status | Evidence |
|---|---|---|---|
| R1 | README with Overview, Architecture, Setup, API Reference, Testing | Met | `README.md` has exactly those five sections |
| R2 | Layered source; no class mixing API, logic and data access | Met | `backend/src/main/java/com/chubb/claims/{api,domain,service,infrastructure}`; `LayeredArchitectureTest` |
| R3 | Dedicated test folder with unit and integration tests that pass | Met | `backend/src/test` (78 passing); frontend specs sit beside their components, as Angular projects do, plus `frontend/e2e` |
| R4 | `resources/` with `application.yml`, `db/migration/`, `openapi/` | Met | `backend/src/main/resources` |
| R5 | OpenAPI YAML contract, optionally a Postman collection | Met | `claims-api.yaml`; `postman/claims-api.postman_collection.json` |
| R6 | `docker-compose.yml` running the whole stack with one command | Met | PostgreSQL 18, Kafka, backend and frontend; `docker compose up --build` |
| R7 | `NOTES.md` with decisions and assumptions | Met | 24 decisions, 11 assumptions, 6 recorded conflicts |
| R8 | `prompts/` with every prompt used | Met | `00-master-prompt.md` to `04-record-prompts-and-confirm.md`, and the candidate's `README.md` on how the master prompt was built |
| R9 | `.claude/ai-journal.md` | Met | Entries 01 to 11; the candidate's review is filled in on 01 to 10, and 11 is awaiting it |
| R10 | Short architecture diagram | Met | Mermaid diagrams in `docs/architecture.md` |
| R11 | `.gitignore`; no secrets committed | Met | See repository hygiene above |
| R12 | Commit messages that describe intent | Met | Every commit states what changed and why, in build order |
| R13 | Short walkthrough for the reviewer | Met | This document |
| R14 | Consistent naming; no junk files in the root | Met | Root holds only the required files and folders |
| R15 | Runnable in under five minutes | Met (measured 80 seconds with warm download caches; about 2.5 to 3.5 minutes estimated when nothing is cached) | Verification above |
| R16 | Built with an LLM and examiner-ready | Met | Journal and prompts; every suite passing from a fresh clone |
| R17 | Time box of three to five hours | Not evidenced | Built by an AI agent in two sessions on one day; commit timestamps are the only record (assumption 9) |

### Requirements in `docs/requirements.md`

| IDs | Status | Notes |
|---|---|---|
| B1 to B8, B10 to B13 | Met | Covered by backend integration tests, the end-to-end lifecycle test and the Postman run |
| B9 | Met, with a narrow reading | "Performance" is shown as closed claims per officer; time-to-decision is next step 12 (assumption 6) |
| B14 to B19 | Deferred, as planned | Reasons in `requirements.md` |
| E1 to E8 | Met | E6: 78 backend tests passing |
| F1 to F8, F10 | Met | F10: 83 component and unit tests, 14 end-to-end tests |
| F9 | Met | Labels (every end-to-end selector uses them), keyboard use (tested), contrast (computed, and axe-core finds no WCAG 2.1 AA violations in either theme). Not tested with a screen reader. |
| F11, F12 | Deferred, as planned | Reasons in `requirements.md` |
| D1 to D11 | Met | See R1 to R16 above |
| D12 | Not evidenced | Same as R17 |
