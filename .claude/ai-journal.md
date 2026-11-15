# AI working journal

A running record of the AI-assisted build of the Chubb APAC claims platform, written as the work happened.

- **AI used:** Claude in Claude Code, working autonomously from the prompts in `prompts/`. Two models were used, one
  per session, as the `Co-Authored-By` lines on the commits show: Claude Fable 5.2 (`claude-fable-5-2`) for entries
  01 to 06 (design documents and backend), and Claude Opus 5.5 (`claude-opus-5-5`) from entry 07 onwards (frontend,
  Docker, verification, documentation and publishing).
- **How to read an entry:** each entry records the instruction being worked on, what the AI produced, the
  alternatives it weighed, and any place it changed approach or corrected its own output.
- **Candidate review:** the line at the end of every entry is reserved for the candidate. It records what was
  accepted, challenged or overridden. The AI leaves it empty.

---

## Entry 01 — 2026-11-15 13:54 IST — Read the inputs

**Instruction:** `prompts/00-master-prompt.md`, "Inputs": read the brief, the structure file and the slide deck in full
before doing anything else.

**Produced:** No files. Read both Markdown inputs and extracted the three slides with python-pptx. Checked the local
toolchain: Java 21 and 25, Maven 3.9.13, Node 22.18, Docker 29.4.3 (the daemon was stopped, so Docker Desktop was
started), Angular CLI 22.2 and Native Federation 22.2 on npm.

**Observations that shaped the plan:**
- The brief says "both use the same Angular app" and suggests a small frontend; the master prompt fixes a
  micro-frontend architecture, and the rubric (slide 3) awards 15 points for it. Treated as a conflict to record, not
  to resolve silently: the shell is the "one app" a user opens, and the micro-frontends are how it is split internally.
- The brief suggests leaving out real authentication; the master prompt asks for role-based access with a simple,
  documented approach. Chose a middle path (see Entry 03).
- The structure file's time box (3–5 hours) cannot be evidenced by an autonomous AI session; recorded as an assumption
  for the candidate to speak to.

**Alternatives considered:** Reading the slide XML directly instead of python-pptx; python-pptx was already installed
and also walks grouped shapes, so it was used.

**Corrections:** The first extraction ran Python in isolated mode (`-I`), which hid the user-installed python-pptx
package and failed. Re-ran without isolation.

**Candidate review:**
I agree with treating the shell as the single user-facing app while using micro-frontends internally. The JWT approach is also more credible than a freely selectable role header.

---

## Entry 02 — 2026-11-15 14:00 IST — Repository setup

**Instruction:** Step 1: new Git repository in `chubb-claims-platform/`, structure-file layout, `.gitignore` before the
first commit, save the prompt, start this journal.

**Produced:** `.gitignore`, `.env.example`, `prompts/00-master-prompt.md` (verbatim), this journal, and the empty
`backend/` and `frontend/` skeleton.

**Alternatives considered:**
- Committing a populated `.env` for convenience. Rejected: `docker-compose.yml` carries safe local defaults through
  `${VAR:-default}` instead, so the examiner needs no extra file and no `.env` is ever committed.
- Ignoring the whole `.claude/` folder (a common default). Rejected because R9 requires `.claude/ai-journal.md` to be
  shared; only `settings.local.json` is ignored.

**Corrections:** None.

**Candidate review:**
The repository setup is appropriate and transparent. Keeping safe defaults while excluding secrets and generated files makes the project easy to run and review.

---

## Entry 03 — 2026-11-15 14:20 IST — Upfront design

**Instruction:** Step 2: write `docs/requirements.md`, `docs/architecture.md`, `NOTES.md` and the OpenAPI contract
before any feature code.

**Produced:** The four files. 51 requirements traced to their source (43 must-have, 8 deferred with reasons). 19 decisions, 9 assumptions and 6 recorded conflicts in `NOTES.md`. The
contract has 11 operations and one error schema.

**Alternatives considered (full reasoning in `NOTES.md`):**
- *Several microservices* versus one service. Chose one, with the exposure projection coupled only through Kafka (D2).
- *Publish to Kafka directly* versus a transactional outbox. Chose the outbox; the dual-write problem is the first
  thing a panel asks about (D6).
- *Compute exposure with a SQL SUM on demand* versus an event-built read model. The SUM is simpler and I said so in
  D7; the read model was chosen because the brief explicitly suggests a consumer that maintains the summary.
- *A role-switch header* (as the brief suggests) versus real role checks. Chose a small JWT login (D9), because a
  header anyone can set cannot honestly be called role-based access.
- *Module Federation* versus Native Federation. Chose Native Federation because Angular 22's default builder is
  esbuild (D13).
- *NgRx* versus signal stores. Chose signal stores for this size (D15).

**Corrections to my own output:**
- My first draft of the event carried only the new status. Reworked it to carry previous and new outstanding amounts,
  so the consumer can apply a difference and never has to call the claims service back.
- I first wrote "claimed amount" as the exposure basis, then replaced it with an explicit `reserve_amount`, because
  after approval the liability is the approved amount, not what was asked for.
- I had seed data as SQL migrations; changed to a seeder that calls the real services (D18) so seeded claims have
  real history and events.

**Candidate review:**
I agree with the outbox, exposure projection and reserve_amount decisions. The documented assumptions and conflicts make the design process clear.

---

## Entry 04 — 2026-11-15 14:35 IST — Backend: domain model and workflow

**Instruction:** Step 3, backend first: strict layering, the claim status workflow, versioned migrations.

**Produced:** `domain/` (Claim aggregate, ClaimStatus transition table, status history, information requests,
ClaimStatusChanged event, repository ports, exceptions), Flyway migrations V1 and V2, and 30 unit tests for the
workflow.

**Alternatives considered:**
- *Spring Boot 3.5 versus 4.1.* I know 3.5 best, but its open-source support ended in mid-2026 and 4.1.2 is current.
  Starting a new service on an unsupported line is hard to defend, so I used 4.1.2 and took the starter names from a
  Spring Initializr skeleton (generated with placeholder names) rather than from memory.
- *Status as a field services can set* versus methods on the aggregate. Chose methods (`pickUp`, `approve`, ...), with
  no public setter, so an illegal transition cannot be written by accident.
- *Returning events from each method* versus the aggregate collecting them (`pullEvents`). Chose collecting, so every
  method has the same shape and the factory method can raise an event too.

**Corrections to my own output:**
- `approve` first changed the reserve and then captured the "previous outstanding" figure, which would have made the
  event report no change. Reordered so the previous figure is captured before any field changes; a unit test now pins
  the 4,200 to 3,800 case.
- I had "today" as the UTC date for the future-incident check. A claimant in Sydney at 08:00 is still on the previous
  UTC day and would have been refused. Changed to the latest calendar date anywhere and added a test for it.

**Candidate review:**
The domain-first approach and aggregate-controlled status transitions are sound. The corrections to amount tracking and timezone handling improved the workflow’s reliability.

---

## Entry 05 — 2026-11-15 15:10 IST — Backend: services, persistence, security, API, events

**Instruction:** Step 3: endpoints per the contract, validation, one error format, Kafka producer and consumer, seed
data, role-based access.

**Produced:** `service/` (five use-case services, an access policy, immutable view records), `infrastructure/`
(Spring Data adapters, transactional outbox and relay, Kafka listener with dead-letter handling, JWT security, demo
seeder) and `api/` (five controllers, DTO records, one exception handler).

**Alternatives considered:**
- *Returning JPA entities from services* versus view records. Entities would be read by controllers after the
  transaction closed (lazy-loading failures) or need open-session-in-view. Chose view records built inside the
  transaction, at the cost of one more mapping step.
- *Separate adapter classes for each repository port* versus a Spring Data interface that extends the port. Chose the
  latter: no hand-written adapter code, and the service layer still sees only the port.
- *`@PreAuthorize` on each controller method* versus one table of URL rules. Chose the table in `SecurityConfig` so
  the whole role matrix is reviewable in one screen; ownership and assignment rules stay in the service layer because
  they need the claim.
- *Rejecting a missing approved amount as a 400* versus a 422. Left it as a business rule (422) in the aggregate, so
  the rule lives in one place.

**Corrections to my own output:**
- The services were written in one pass and compiled, so the history commits them in dependency order rather than in
  the order typed. The commit order is honest about structure, not about minute-by-minute sequence.
- A leftover unused helper (`DashboardService.zero()`) was removed before the first compile.

**Candidate review:**
The layering and immutable view records keep persistence concerns out of the API. Centralizing endpoint permissions while enforcing ownership in services is a sensible balance.

---

## Entry 06 — 2026-11-15 15:40 IST — Backend tests, and what they caught

**Instruction:** Step 3 tests: unit tests for workflow and service rules, integration tests for the main endpoints
and the Kafka event flow.

**Produced:** 78 tests, all passing (`mvn test`, 17 seconds with warm containers): 58 unit tests including an
ArchUnit layering test, and 20 integration tests on real PostgreSQL 18 and Kafka 4.2 through Testcontainers. Every
integration request is validated against `claims-api.yaml`.

**What the tests caught in my own work (each fixed at the cause):**
1. *Schema mismatch.* Hibernate's schema validation refused to start: the migrations used `CHAR(2)`/`CHAR(3)` and the
   entities map to `VARCHAR`. Changed the migrations (they had never been released) rather than loosening validation.
2. *Invalid YAML in the contract.* A description containing a comma inside an inline map was parsed as an extra key.
   Quoted it. The contract validator found this; nothing else would have until the frontend types were generated.
3. *Contract and code disagreed on a nullable enum.* `fromStatus` is null on the first history entry, but the contract
   declared it through a reference that did not allow null. Fixed the contract.
4. *Validator and `allOf`.* The validator rejects fields of one `allOf` branch while checking the other. Followed its
   documented advice and disabled that single check; noted that undeclared extra fields are therefore not detected.
5. *My test was wrong, not the code.* The dead-letter test timed out. Debug logging showed the service had published
   to the dead-letter topic; my test consumer was built from properties that still pointed at `localhost:9092`
   instead of the container. Switched the test to the application's own consumer factory. I checked the evidence
   before touching production code, which would have been the wrong fix.
6. *Mockito misuse.* I created mocks inside a `thenReturn(...)` argument; moved the construction out.

**Alternatives considered:** `@EmbeddedKafka` (faster, no Docker) versus Testcontainers. Chose Testcontainers because
PostgreSQL is needed anyway for the native upsert and `SKIP LOCKED` queries, and one mechanism is simpler than two.

**Not done:** No test forces a true optimistic-lock collision (two commits racing); the 409 mapping for it is code
reviewed only. Listed under next steps.

**Candidate review:**
The combination of unit tests and real PostgreSQL/Kafka integration tests provides good coverage. The remaining optimistic-lock test is a known, clearly documented gap.

---

## Entry 07 — 2026-11-15 14:41 IST — Resumed session: take stock before building

**Instruction:** `prompts/01-resume-session.md`: the master prompt, sent again.

**Produced:** No code. Read the journal, `NOTES.md`, the requirements, the architecture document and the contract
again, and re-ran `mvn test`: 78 tests, 0 failures, 0 errors.

**Alternatives considered:**
- *Start the repository again from Step 1*, as a literal reading of the prompt would suggest. Rejected: the existing
  history already satisfies Steps 1 and 2 and the backend part of Step 3, and rewriting it would destroy the honest
  record of how it was built.
- *Trust the journal's test count.* My first quick count of the surefire reports said 57, not 78. Checked before
  writing anything down: the claim test class reports its nested classes separately, so the summary lines undercount.
  The per-file XML reports add up to 78, which matches entry 06.

**Remaining scope at this point:** the Angular shell, both micro-frontends and the shared library; frontend tests;
Dockerfiles and `docker-compose.yml`; the Postman collection; `README.md` and `docs/walkthrough.md`; verification from
a fresh clone.

**Candidate review:**
Resuming from the existing repository was the right choice and preserved the development history. The test-count discrepancy was responsibly checked and corrected.

---

## Entry 08 — 2026-11-15 15:10 IST — Frontend: workspace, shell, micro-frontends, Docker

**Instruction:** Step 3, frontend: the shell, the claimant and staff micro-frontends and a shared library; typed
against the contract; predictable state; theme, loading, empty and error states; accessibility basics. Then
`docker compose up` for the whole stack.

**Produced:** An Angular 22 workspace with three applications and one library, Native Federation 22, Dockerfiles
for both tiers, nginx configuration and `docker-compose.yml`. The first `docker compose up --build` (including base
image pulls) reached all-healthy in 115 seconds, and signing in as the demo claimant in a browser loaded the claimant
micro-frontend at runtime through the relative federation manifest.

**Alternatives considered:**
- *Forcing openapi-typescript into the workspace* with `--legacy-peer-deps`. It declares TypeScript 5 as a peer and
  Angular 22 needs TypeScript 6. Ran it through `npx` instead, since the generated file is committed (D11) and the
  build never needs the generator.
- *Node 22.18 (installed) with Angular 21* versus *installing Node 24 LTS with nvm for Angular 22*. Angular CLI 22.2
  needs Node 22.22 or later. Installed Node 24 alongside, left the machine's default alone, and pinned it in
  `frontend/.nvmrc`; NOTES D13 already named Angular 22, and the Docker build uses Node 24 anyway.
- *Absolute remote URLs injected at container start* versus *relative paths in the deployed manifest*. Read the
  runtime's URL handling, then tested relative paths in a browser rather than reasoning further; they work, so no
  start-up script is needed.
- *Publishing PostgreSQL and Kafka ports* for local tooling. Rejected: a reviewer with a local PostgreSQL on 5432
  would see `docker compose up` fail. Only ports 4200 and 8080 are published, both overridable.
- *Generic store base class* versus *two small helpers*. Chose `QueryState` (a read) and `CommandState` (a write)
  that every store composes; a base class would hide what each store does.

**Corrections to my own output (each caught before or by a test, not by the reviewer):**
1. The decision form re-suggested the approved amount whenever the claim object changed. The page refreshes the
   claim every ten seconds, so an officer's typed amount would have been overwritten mid-edit. Changed the effect to
   depend on the claim's id only; my first patch still read the claim tracked, and I caught that on re-reading.
2. Pages call `store.open()` from an effect, and `QueryState.load()` read its own state signal, which would have made
   every load trigger another load. Made loads and commands untracked inside the helpers, so any store is safe to
   call from an effect, and made the page effects say so explicitly.
3. The claimant's answer form used `(ngSubmit)` on a bare `<form>`. Without a form directive that event never fires,
   and the button would have done a native submit and reloaded the page. The component test failed on it; fixed the
   component, not the test.
4. A scripted find-and-replace that introduced the money pipe mangled two import statements. The build caught it;
   fixed by hand and then ran Prettier over the workspace.
5. The contrast ratios I wrote into the theme comments were estimates. Computed them; all pairs pass WCAG AA (lowest
   5.4:1) but several comments were off by a few tenths and were corrected.
6. The unit-test builder was pointed at the federation builder and warned about it; pointed it at each
   application's esbuild target.

**Candidate review:**
The Angular, Native Federation and Docker setup meets the architectural goals. The fixes around forms, reactive loading, refresh behavior and keyboard focus addressed important usability issues.

---

## Entry 09 — 2026-11-15 15:55 IST — End-to-end and accessibility tests, verification, documentation

**Instruction:** Step 3 tests (one end-to-end test per key flow), Step 4 (verify from a fresh clone, check R1–R17 and
every requirement, check for secrets and junk) and Step 5 (README, walkthrough, optional Postman collection).

**Produced:** Playwright tests for the lifecycle, rejection and access by role; axe-core scans of every main screen
in both colour schemes and a keyboard test; a Postman collection with 43 assertions; `NOTES.md` decisions D20 to D24
and assumptions 10 and 11; `README.md`; `docs/walkthrough.md` with the verification results and both checklists.

**Results from a fresh clone of the final code:** 80 seconds from `docker compose build --no-cache` to all services
healthy; backend 78 of 78, frontend 83 of 83, end-to-end 14 of 14, Postman 43 of 43 assertions.

**Alternatives considered:**
- *Report the 80-second figure as the examiner's start-up time.* Rejected as misleading: base images and the Maven and
  npm download caches were still warm, and clearing them meant pruning Docker build cache that other projects on this
  machine share. Measured the cold downloads separately in clean containers instead, and reported the cold total as
  an estimate, labelled as one.
- *Treat accessibility as covered by labelled selectors and computed contrast.* Added an automated axe-core scan and
  a keyboard test instead, because "basics" claimed without a check is an assertion, not evidence.
- *Publish PostgreSQL and Kafka ports for local tools.* Kept them internal (D22).

**Corrections to my own output:**
1. The lifecycle test read the policy options before the list had loaded and failed once; the helper now waits for
   the option. The test was wrong, not the app.
2. While moving that helper I cut off the end of the support file, removing the exposure helper; restored it before
   the next run.
3. The keyboard test failed twice. The first failure was my test pressing Tab before the app had rendered. The second
   was a real bug: the shell moved focus to the main region on every navigation, including the query-only navigation
   that switching work-queue tabs performs, so arrow keys threw focus out of the tab list. Traced focus step by step
   before changing anything; fixed the shell to move focus only when the path changes, with unit tests.
4. My first README draft gave a start-up time before it had been measured; replaced it with the measured figure and
   the labelled estimate. The walkthrough also claimed validation errors were "announced" (they are read when focus
   reaches the field) and hard-coded a commit count that would go stale; both corrected.
5. A Prettier-only commit was described as touching one file when it touched two; amended the message before
   anything else was committed on top.

**Found and not fixed (listed under next steps in the walkthrough, as the prompt requires):** published outbox rows
and processed-event ids are never purged; the lists have no paging controls beyond 100 claims; a failed
micro-frontend load needs a full page reload to retry.

**Candidate review:**
The final verification is convincing because it combines automated tests, end-to-end flows, accessibility checks and contract validation. The remaining production-hardening items are clearly documented.

---

## Entry 10 — 2026-11-15 19:12 IST — Publish to GitHub

**Instruction:** After testing the full workflow and seeing every suite pass, publish the repository to GitHub as a
public repository.

**Produced:** `prompts/02-publish-to-github.md`, this entry, and a public repository at
https://github.com/supratik01/chubb-claims-platform with the full commit history on `main`.

**Checks before publishing:** the working tree was clean and had no remote; the assessment inputs (the brief, the
structure file and the slides) sit outside the repository folder and are not tracked; no environment files, build
output or dependencies are tracked; the only credentials in the repository are the documented local defaults.

**Alternatives considered:**
- *Squash the history into one commit before publishing.* Rejected: the small, intent-describing commits are part of
  what the assessment reviews.
- *Publish as private and share access with the reviewers.* Not chosen, because the instruction was a public
  repository.

**Corrections to my own output:** none.

**Candidate review:**
Publishing the clean repository with its full commit history was appropriate for the assessment. The checks confirmed that secrets, generated files and unrelated inputs were not included.

---

## Entry 11 — 2026-11-15 20:55 IST — Commit the candidate's reviews; record the prompts

**Instruction:** `prompts/03-commit-candidate-reviews.md`: commit and push the candidate's new `prompts/README.md`
and the review lines added to this journal. Then `prompts/04-record-prompts-and-confirm.md`: save that prompt as a
numbered file, update the other files that need it, and confirm with the candidate before committing.

**Produced:** commit `13a0a69` with the two candidate-written files, unchanged, pushed to `main`. Then the two prompt
files, this entry, and the walkthrough's R8 and R9 rows brought up to date (they still listed only prompts 00 and 01,
and described every review line as empty).

**Alternatives considered:**
- *Save prompt 03 in the same commit as the candidate's files.* Not done, because the instruction named exactly two
  files; the omission was reported instead, which led to prompt 04.
- *Leave this entry's review line out, since the candidate has already reviewed the build.* Kept it, blank, like
  every other entry.

**Corrections to my own output:**
1. Prompt 03 was not saved when it was given, although the master prompt asks for every later prompt to be saved.
   Saved now, with a note that it was saved late.
2. Entry 10 was appended with a second horizontal rule above it, leaving two in a row; removed one.
3. The journal's header named only `claude-fable-5-2`, the first session's model, although entries 07 onwards were
   written by Claude Opus 5.5. The candidate asked for the header to be corrected; it now names both models and
   which entries each wrote, taken from the commits' `Co-Authored-By` lines.

**Candidate review:**
I agree to that.