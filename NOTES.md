# Notes: decisions and assumptions

One short entry per decision. Each has the context, the decision, the alternatives considered and the trade-off
accepted. Assumptions and conflicts between the input documents are listed at the end.

## Decisions

### D1. Scope: depth on one workflow rather than breadth
- **Context:** The rubric gives 12 of 135 points to functional completeness and far more to design, structure and tests.
- **Decision:** Build one complete claim lifecycle for two lines of business, with production-style structure, and
  defer uploads, notifications, market rules and real identity (see `docs/requirements.md`).
- **Alternatives:** Stub many features thinly.
- **Trade-off:** Fewer features to demo; every one that exists is tested and defensible.

### D2. One backend service (modular monolith)
- **Context:** The role profile lists microservices; the brief asks "one service or several, and why?".
- **Decision:** One Spring Boot service with strict internal layers. The exposure projection is coupled to the rest
  only through Kafka, so it can be extracted later.
- **Alternatives:** Separate claims, workflow, reporting and identity services.
- **Trade-off:** No independent scaling or deployment per capability yet. In return there are no distributed
  transactions, one database and a stack that starts in minutes.

### D3. Layering with the domain at the centre
- **Context:** R2 requires `api/`, `domain/`, `service/`, `infrastructure/` with no mixed classes.
- **Decision:** `domain` holds the entities, the status workflow and repository interfaces (ports) and depends on no
  other layer. `service` holds use cases. `api` holds controllers and DTOs. `infrastructure` implements the ports
  (Spring Data, Kafka, security). An ArchUnit test enforces the direction of dependencies.
- **Alternatives:** Full hexagonal architecture with separate persistence models and mappers.
- **Trade-off:** Domain entities carry JPA annotations, and repository ports use Spring Data's `Page` type. That is a
  small, deliberate leak that avoids a second set of model classes and mapping code.

### D4. Workflow rules live on the `Claim` aggregate
- **Context:** The status flow is the core business rule.
- **Decision:** `ClaimStatus` declares the allowed transitions; `Claim` exposes intent-revealing methods (`pickUp`,
  `requestInformation`, `approve`, and so on) that enforce them and write the history. Services cannot set a status
  directly.
- **Alternatives:** A state machine library (Spring Statemachine); status checks spread through services.
- **Trade-off:** A hand-written transition table must be kept in step with the diagram. It is nine lines and unit
  tested, which is cheaper than a framework.

### D5. REST for user-facing operations, Kafka for consequences
- **Context:** The brief requires both and an explanation of the split.
- **Decision:** Anything a person waits on is REST. Kafka carries `ClaimStatusChanged` events that maintain the
  exposure read model (and would drive notifications). The full table is in `docs/architecture.md` section 3.
- **Alternatives:** Accept commands over Kafka (asynchronous command processing); compute everything synchronously.
- **Trade-off:** Exposure figures are eventually consistent, typically within a second. Commands stay simple and give
  immediate validation feedback.

### D6. Transactional outbox for publishing events
- **Context:** Saving a claim and sending to Kafka are two systems; doing both directly can lose or invent events.
- **Decision:** The service writes the event to an `outbox_event` table in the same transaction as the claim. A
  scheduled relay publishes pending rows and marks them published.
- **Alternatives:** Send to Kafka inside the transaction; send after commit with a transactional event listener;
  change data capture with Debezium.
- **Trade-off:** Up to half a second of extra latency and a polling query. Delivery is at-least-once, so consumers
  must be idempotent (D7). Debezium would remove the polling but adds a component the examiner would have to run.

### D7. Idempotent, delta-based exposure projection
- **Context:** At-least-once delivery means an event can arrive twice.
- **Decision:** Each event carries the previous and new outstanding amount. The consumer records the event identifier
  in `processed_event` and applies the difference to `exposure_summary` in one transaction. A repeat is skipped.
- **Alternatives:** Recompute the total with a query on every event; compute exposure on demand with `SUM` over claims.
- **Trade-off:** A read model can drift if a bug is ever shipped in the consumer; it can be rebuilt by replaying the
  topic. An on-demand `SUM` would be simpler and adequate at this data size, but would not demonstrate the
  event-driven read model the brief asks for and would not scale to a separate reporting service.

### D8. Reserve amount as the basis for exposure
- **Context:** "Outstanding liability exposure" must be defined to be tracked.
- **Decision:** Each claim has a `reserve_amount`: claimed amount (capped at the sum insured) on submission, approved
  amount on approval, zero on rejection or settlement. Exposure is the sum of reserves on open claims, per currency.
- **Alternatives:** Use the claimed amount throughout; let officers set reserves manually.
- **Trade-off:** Real insurers set case reserves by assessment. A manual reserve adjustment is listed as a next step.

### D9. Authentication: login endpoint issuing signed JWTs for seeded users
- **Context:** The brief says real authentication can be left out; the prompt asks for role-based access with a
  simple, documented approach.
- **Decision:** `POST /api/v1/auth/login` checks a BCrypt password hash and returns an HMAC-signed JWT (one hour)
  carrying the user id and role. Spring Security's resource-server support validates it. Three roles: `CLAIMANT`,
  `CLAIMS_OFFICER`, `CLAIMS_MANAGER`. Ownership rules (a claimant sees only their own claims) are enforced in the
  service layer.
- **Alternatives:** A role-switch header with no login; HTTP Basic; Keycloak in Docker.
- **Trade-off:** No refresh tokens, lockout or password reset. Keycloak would be closer to production but adds
  start-up time and configuration to the examiner's run. Because the API only trusts standard JWTs, moving to an
  identity provider changes configuration, not controllers.

### D10. One error format: RFC 9457 problem details
- **Context:** The prompt asks for one consistent error format.
- **Decision:** Every error, including 401 and 403 from the security filter, is `application/problem+json` with a
  stable machine-readable `code` and, for validation failures, an `errors` list of field and message.
- **Alternatives:** A custom envelope.
- **Trade-off:** None significant; it is a standard that Spring supports directly.

### D11. Contract-first API, verified in both tiers
- **Context:** "Who defines the API contract?"
- **Decision:** A hand-written OpenAPI file is the source of truth. Backend integration tests validate real responses
  against it; frontend types are generated from it and committed.
- **Alternatives:** Generate the specification from controller annotations (code-first); generate controller
  interfaces and DTOs from the specification.
- **Trade-off:** Hand-written controllers could drift, which the validation tests guard against. Generating server
  code was rejected because generated DTOs are awkward to read and add build time. The generated TypeScript file is
  committed so the frontend Docker build does not need the backend folder; `npm run generate:api` refreshes it.

### D12. Optimistic locking on claims
- **Context:** Two officers can try to pick up the same claim.
- **Decision:** `claim.version` with JPA optimistic locking; the loser gets a 409 problem response.
- **Alternatives:** Pessimistic row locks.
- **Trade-off:** The losing user must retry, which is the right behaviour for a rare human-speed conflict.

### D13. Native Federation for the micro-frontends
- **Context:** The prompt fixes a shell plus two micro-frontends and asks for a choice between Native Federation and
  Module Federation.
- **Decision:** Native Federation (`@angular-architects/native-federation` 22) on Angular 22.
- **Alternatives:** Webpack Module Federation; separate applications linked by URL; iframes.
- **Trade-off:** Angular's default build system is now esbuild-based, and Module Federation needs the older webpack
  builder, which is slower and off the framework's main path. Native Federation uses browser-standard ES modules and
  import maps and keeps the default builder. Its ecosystem is smaller than webpack's, and all applications must stay
  on compatible Angular versions because they share one copy of the framework.

### D14. One workspace, three applications, one shared library
- **Context:** The micro-frontends need shared components, theme and session.
- **Decision:** A single Angular workspace with `shell`, `claimant` and `staff` applications and a `shared` library
  shared as a federation singleton. The micro-frontends never import each other, and the library holds no feature
  state.
- **Alternatives:** Separate repositories with the library published to a package registry.
- **Trade-off:** One repository means one dependency set and atomic changes, at the cost of teams not being fully
  independent. Each application still builds and deploys separately.

### D15. Signal-based stores instead of NgRx
- **Context:** The rubric scores predictable state and clean data flow.
- **Decision:** One small store per screen built on Angular signals, each exposing a read-only `RequestState`
  (loading, loaded, error) and methods for commands. Only the session is global.
- **Alternatives:** NgRx Store with actions, reducers and effects; NgRx SignalStore; state in components.
- **Trade-off:** No time-travel debugging or enforced action log. For a handful of screens with server-owned state,
  NgRx would add more boilerplate than the features themselves.

### D16. Background refresh instead of server push
- **Context:** Claim status changes while the claimant is not acting.
- **Decision:** Lists and the open claim refresh every ten seconds while visible.
- **Alternatives:** Server-sent events or WebSockets fed from Kafka.
- **Trade-off:** Up to ten seconds of staleness and some extra requests. Push needs connection management and a
  per-user event fan-out, which is not justified for this scope.

### D17. One frontend container serving the shell and both micro-frontends
- **Context:** The stack must start with one command in under five minutes.
- **Decision:** One nginx container serves the shell at `/`, the micro-frontends under `/mfe/claimant/` and
  `/mfe/staff/`, and proxies `/api` to the backend.
- **Alternatives:** One container per micro-frontend on separate ports.
- **Trade-off:** In Docker the three builds are deployed together, which hides their independence. The federation
  manifest is plain runtime configuration, so pointing a micro-frontend at its own host is a one-line change.
  A single origin also removes the need for cross-origin configuration.

### D18. Demo data seeded through the real services
- **Context:** A demo needs users, policies and claims in several states.
- **Decision:** A `demo` profile runs a seeder that creates users and policies and then drives claims through the
  same service methods the API uses, so history, outbox events and the exposure summary are produced the normal way.
- **Alternatives:** SQL seed scripts.
- **Trade-off:** Seeding runs at application start (only when the database is empty). SQL scripts would duplicate
  business rules and could insert states the workflow would never allow.

### D19. Ephemeral data in Docker
- **Context:** The examiner should get the same demo every time.
- **Decision:** PostgreSQL and Kafka run without named volumes; `docker compose down` followed by `up` gives a clean,
  freshly seeded system.
- **Alternatives:** Persistent volumes.
- **Trade-off:** Data is lost on `down`. That is the desired behaviour for a demo and wrong for anything else.

## Assumptions

1. **Markets.** The brief says six countries without naming them. Assumed Singapore, Hong Kong, Australia, Malaysia,
   Thailand and Indonesia. Country and currency live on the policy; no market-specific rules are applied.
2. **Managers are staff with one extra view.** The brief names two user types and separately says managers cannot see
   exposure. Modelled as a third role that can do everything an officer can, plus see exposure.
3. **Who can act on a claim.** Only the assigned officer, or a manager, can request information, decide or settle.
   Any staff member can pick up an unassigned claim.
4. **Policies exist already.** Policy administration is out of scope; policies are seeded. A claimant can only claim
   against their own policy, and only for an incident inside the cover period.
5. **Currencies are not converted.** Exposure is reported per currency.
6. **"Performance" in the workload view** means closed-claim counts per officer. Time-to-decision metrics are a next step.
7. **Answering an information request is text only**, because uploads are deferred.
8. **Settlement records that payment happened.** No payment system is called.
9. **Time box.** The build was done by an AI agent in one autonomous session; the three-to-five-hour limit in R17
   cannot be evidenced from the repository beyond commit timestamps.

## Conflicts between the inputs

| Topic | Sources in conflict | Resolution |
|---|---|---|
| Frontend shape | The brief says both user types use "the same Angular app" and suggests keeping the frontend small. The master prompt and the rubric (slide 3) require a micro-frontend architecture. | Micro-frontends behind one shell: one address and one login for the user, split internally by user type. |
| Frontend effort | The brief says to do the frontend only if time allows. The rubric gives the frontend 45 points. | Backend built first, then the full frontend. |
| Authentication | The brief suggests a simple role switch and leaving real authentication out. The master prompt asks for role-based access; slide 2 lists authentication and authorisation. | Simple token login with seeded users and real server-side role checks (D9). |
| Journal location | The brief says "a log file in the repo". The structure file fixes `.claude/ai-journal.md`. | Followed the structure file for layout. |
| Walkthrough location | The structure file's sample layout puts "diagram + walkthrough" in `docs/architecture.md`. The master prompt asks for `docs/walkthrough.md`. | Diagram in `docs/architecture.md`, walkthrough in its own file, both linked from the README. |
| Time box | The brief targets two to three hours with a cap of five. The structure file says three to five. | Recorded; see assumption 9. |
