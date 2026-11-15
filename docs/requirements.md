# Requirements

Consolidated from the three input documents. Build order follows the numbering: backend first, then frontend.

**Sources**

| Key | Document |
|---|---|
| BRIEF | `Chubb APAC Fullstack Assessment — What to Build.md` |
| STRUCT | `How_to_Structure_a_Take_Home_Assesment.md` (requirements R1–R17) |
| DECK | `Chubb_Weekend_Drive_Java.pptx` (slide 1 full-stack profile, slide 2 backend profile, slide 3 scoring rubric) |
| PROMPT | `prompts/00-master-prompt.md` (the candidate's fixed decisions) |

**Priority:** *Must* is built in this submission. *Deferred* is deliberately left out, with the reason given.

## 1. Backend functional requirements

| ID | Requirement | Source | Priority | Notes |
|---|---|---|---|---|
| B1 | A claimant can report an incident against one of their own policies (motor or property). | BRIEF "Who uses it"; DECK slide 1 "Role scope" | Must | |
| B2 | A claimant can list and track their own claims, including the full status history. | BRIEF "Who uses it"; DECK slide 1 | Must | |
| B3 | A claimant can answer a request for more information. | BRIEF "Who uses it" ("upload or answer") | Must | Text answer only; see B14. |
| B4 | A claimant can see the final decision, the approved amount and the reason. | BRIEF "Who uses it" | Must | |
| B5 | Staff can see new, unassigned claims and pick one up. | BRIEF "Who uses it"; BRIEF "Suggested scope" (assign endpoint) | Must | Picking up assigns the claim to the caller. |
| B6 | Staff can review a claim, request more information, approve or reject it, and settle an approved claim. | BRIEF "Who uses it"; DECK slide 1 ("assess, settle or reject") | Must | |
| B7 | Claims follow an explicit status flow: Submitted, Under Review, Info Requested, Approved or Rejected, Settled. Illegal moves are refused. | BRIEF "Suggested scope" | Must | |
| B8 | Every status change is recorded in a status history (who, when, from, to, note). | BRIEF "questions to think about" (status history) | Must | |
| B9 | Staff can see the team's workload and performance. | BRIEF "Who uses it"; DECK slide 1 ("workload view") | Must | Counts per officer and status, plus the unassigned backlog. |
| B10 | Managers can see how many claims are open and the outstanding liability exposure. | BRIEF "The problem" (managers); BRIEF "questions" (outstanding money) | Must | Broken down by country, line of business and currency. |
| B11 | Login with roles; each role reaches only its own operations and data. | BRIEF "Who uses it"; DECK slide 2 ("authN / authZ, secure coding"); PROMPT Step 3 | Must | Simple token login with seeded demo users. |
| B12 | REST for operations a user waits on; Kafka for work that can happen in the background; the split is documented. | BRIEF "Technology rules"; DECK slides 1–2; PROMPT "Fixed decisions" | Must | |
| B13 | Each status change publishes a Kafka event; a consumer maintains the outstanding exposure summary. | BRIEF "Suggested scope" | Must | |
| B14 | Document and photo upload for claims. | BRIEF "Who uses it" ("upload") | Deferred | BRIEF "Leave out on purpose" lists file uploads. Needs object storage, virus scanning and size limits to do properly. |
| B15 | Notifications to claimants (email, SMS, push) on status change. | BRIEF "Technology rules" (example) | Deferred | BRIEF "Leave out on purpose". The event that would drive it is published; only the consumer is missing. |
| B16 | Market-specific rules for the six countries (regulation, tax, language). | BRIEF "The problem" | Deferred | BRIEF "Leave out on purpose". Country and currency are carried on every policy and event so rules can be added later. |
| B17 | Production identity (single sign-on, password reset, account lifecycle). | DECK slide 2 | Deferred | BRIEF "Leave out on purpose" (real authentication). Tokens are standard signed JWTs, so an identity provider can replace the login endpoint. |
| B18 | Multiple microservices, Kubernetes manifests and a CI/CD pipeline. | DECK slides 1–2 (tech stack) | Deferred | Stack keywords from the role profile, not asked for by the brief. One service is the right size here (see `NOTES.md` D2). |
| B19 | Caching. | DECK slide 1 ("Data & APIs: databases, caching") | Deferred | No measured need at this size. The exposure read model already makes the expensive dashboard query cheap. |

## 2. Backend engineering requirements

| ID | Requirement | Source | Priority | Notes |
|---|---|---|---|---|
| E1 | Java and Spring Boot, built with Maven. | PROMPT; DECK slides 1–2 | Must | |
| E2 | Layered source: `api/`, `domain/`, `service/`, `infrastructure/`; no class mixes API handling, business logic and data access. | STRUCT R2; DECK slide 3 | Must | Enforced by an automated architecture test. |
| E3 | PostgreSQL with versioned migrations under `resources/db/migration`. | STRUCT R4; PROMPT | Must | Flyway. |
| E4 | API contract as OpenAPI YAML under `resources/openapi`, written before the controllers. | STRUCT R4, R5; BRIEF "questions"; PROMPT Step 2 | Must | |
| E5 | Input validation and one consistent error response format. | PROMPT Step 3; DECK slide 3 ("error handling") | Must | RFC 9457 problem details. |
| E6 | Unit tests for the status workflow and service rules; integration tests for the main endpoints and the Kafka flow; all passing. | STRUCT R3; PROMPT Step 3; DECK slide 3 | Must | |
| E7 | Seed data for a demo. | PROMPT Step 3 | Must | |
| E8 | Secrets from environment variables with safe local defaults; none committed. | STRUCT R11; PROMPT Step 1 | Must | |

## 3. Frontend requirements

| ID | Requirement | Source | Priority | Notes |
|---|---|---|---|---|
| F1 | Angular; one application serves both user types, with role-based routing. | BRIEF "Technology rules", "Who uses it"; DECK slide 1 | Must | |
| F2 | Micro-frontend architecture: a shell, a claimant micro-frontend, a staff micro-frontend and a shared component and theme library. | PROMPT "Fixed decisions"; DECK slide 3 | Must | Conflict with the brief's "same app" recorded in `NOTES.md`. |
| F3 | Claimant screens: report an incident, my claims, claim detail with status timeline, answer an information request, see the decision. | BRIEF "Who uses it", "Suggested scope" | Must | |
| F4 | Staff screens: work queue (unassigned, mine, all), claim detail with pick up, request information, approve, reject and settle. | BRIEF "Who uses it", "Suggested scope" | Must | |
| F5 | Staff workload view and manager exposure view. | BRIEF "Who uses it", "The problem" | Must | |
| F6 | Typed against the OpenAPI contract. | PROMPT Step 3 | Must | Types generated from the YAML. |
| F7 | Predictable state management with clean data flow. | PROMPT Step 3; DECK slide 3 | Must | |
| F8 | Consistent theme; loading, empty and error states. | PROMPT Step 3; DECK slide 3 | Must | |
| F9 | Accessibility basics: labels, keyboard navigation, contrast. | PROMPT Step 3; DECK slide 3 | Must | |
| F10 | Component tests for key components and one end-to-end test per key flow. | PROMPT Step 3; DECK slide 3 | Must | |
| F11 | Live push of status changes to the browser (WebSocket or server-sent events). | BRIEF "questions" (slow, asynchronous processing) | Deferred | Periodic refresh shows the same information with far less machinery; push is listed under next steps. |
| F12 | Translations and locale-specific formats for the six markets. | BRIEF "The problem" | Deferred | Same reason as B16. |

## 4. Delivery requirements

| ID | Requirement | Source | Priority | Notes |
|---|---|---|---|---|
| D1 | Root `README.md` with sections Overview, Architecture, Setup, API Reference, Testing. | STRUCT R1 | Must | |
| D2 | `docker-compose.yml` starts PostgreSQL, Kafka, backend and frontend with one command, in under five minutes. | STRUCT R6, R15; BRIEF "What you hand in" | Must | |
| D3 | `NOTES.md` with decisions and assumptions. | STRUCT R7 | Must | |
| D4 | `prompts/` with every prompt used. | STRUCT R8 | Must | |
| D5 | `.claude/ai-journal.md`, written as the work happens, with room for the candidate's review. | STRUCT R9; BRIEF "What you hand in" | Must | |
| D6 | Short architecture diagram. | STRUCT R10 | Must | |
| D7 | `.gitignore`; no secrets, build output or junk files. | STRUCT R11, R14 | Must | |
| D8 | Small commits with messages that describe intent. | STRUCT R12; BRIEF "What you hand in" | Must | |
| D9 | Walkthrough document for the reviewer and the interview. | STRUCT R13; BRIEF "What you hand in" | Must | |
| D10 | Postman collection. | STRUCT R5 (optional) | Must | Small, and useful for the demo. |
| D11 | Built with an LLM; submitted only when examiner-ready. | STRUCT R16 | Must | |
| D12 | Time box of three to five hours. | STRUCT R17; BRIEF "What this is" | Must | Cannot be proven from inside the repository; see `NOTES.md` assumptions. |
