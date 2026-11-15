<!-- Saved verbatim from the session on 2026-11-15. The prompt was accompanied by one screenshot of the
     example backend layout: src/{api,domain,service,infrastructure}, test/, resources/{application.yml,db/migration,openapi}. -->

# Chubb APAC claims platform

You are a senior full-stack engineer with more than ten years of experience in Java/Spring Boot and Angular. You build clean, well-structured systems and can defend every design decision.

## Why this matters

The submission is scored out of 135 points:

| Area | Points | What earns them |
|---|---|---|
| Planning, architecture and design thinking | 15 | Upfront plan, design decisions and trade-offs |
| Service and API design | 10 | Clear endpoints, contracts and error handling |
| Clean architecture and engineering discipline | 15 | Layering, separation of concerns, readable code |
| Backend test automation | 8 | Unit and integration tests with meaningful coverage |
| Data and integration implementation | 12 | Data model, persistence and service integration |
| Backend functional completeness | 5 | Required backend features working end to end |
| Micro-frontend architecture and component design | 15 | Micro-frontend structure and reusable components |
| Frontend test automation | 7 | Component and end-to-end tests for key flows |
| State management and data flow | 8 | Predictable state and clean data flow to the UI |
| Theming, accessibility and UX | 8 | Consistent theming, accessible and usable interface |
| Frontend functional completeness | 7 | Required UI features working end to end |
| AI collaboration process | 25 | How the AI was directed, reviewed and documented across the build |

Use this table to decide where effort goes. Design, architecture and clean structure are worth far more than feature count: functional completeness is only 12 of 135 points. The AI collaboration process is the largest single area, so the prompts and journal are as important as the code.

## Inputs

Everything is in `/Users/supratik/Projects/chubb-apac`. Read these in full before doing anything else:

- `Chubb APAC Fullstack Assessment — What to Build.md`: the assessment brief in plain English. It covers the problem, the two user types, the technology rules, the deliverables, and the walkthrough format.
- `How_to_Structure_a_Take_Home_Assesment.md`: the required repository structure, requirements R1–R17. This is the checklist the examiner reviews against. Follow its layout exactly, including `NOTES.md`, `prompts/` and `.claude/ai-journal.md`.
- `Chubb_Weekend_Drive_Java.pptx`: slides 1 and 2 are the role profiles, which list the expected tech stack and skills. Slide 3 is the scoring rubric reproduced above. Extract the text with python-pptx or by reading the slide XML.

Where these sources leave something open, make the call a careful senior engineer would make and record it in `NOTES.md`. Where they conflict, follow the brief for what to build and the structure file for how to lay it out, and record the conflict.

## Fixed decisions

- Backend: Java with Spring Boot, built with Maven.
- Frontend: Angular, with a micro-frontend architecture: a shell application plus separate micro-frontends for the claimant area and the claims-staff area, sharing a small component and theme library. Use Native Federation or Module Federation; choose one and record why in `NOTES.md`.
- Communication: REST for anything a user waits on; Kafka for events that can be processed asynchronously. Record which operations use which, and why.
- Database: PostgreSQL with versioned migrations (Flyway or Liquibase).
- The whole stack, including PostgreSQL, Kafka, the backend and the frontend, starts with `docker compose up` and runs on an examiner's machine in under 5 minutes.

## Step 1: Set up the repository and the AI record

1. Create a new Git repository in a new subfolder, `chubb-claims-platform/`, inside the project folder. Don't change the existing files in the project folder.
2. Lay it out as the structure file specifies, with `backend/` (Spring Boot, layered into `api/`, `domain/`, `service/`, `infrastructure/`) and `frontend/` (Angular shell and micro-frontends).
3. Add a `.gitignore` before the first commit, so build output, `node_modules`, IDE files and secrets are never committed. Configuration secrets come from environment variables with safe local defaults.
4. Save this prompt as `prompts/00-master-prompt.md`. Save every later prompt you are given during the build as the next numbered file in `prompts/`.
5. Start `.claude/ai-journal.md` now and add to it as you work, not at the end. Each entry has a timestamp, the instruction being worked on, what you produced, the alternatives you considered, and where you changed approach or corrected your own output and why. Give every entry an empty "Candidate review" line. I fill those in myself with what I accepted, challenged or overrode; never fill them in for me.

## Step 2: Plan and design

Before writing feature code, write these files and commit them:

- `docs/requirements.md`: the requirements consolidated from the three inputs. Each one cites its source file (and slide or R-number), and is marked must-have or deferred, with a reason for each deferral.
- `docs/architecture.md`: a short architecture diagram (Mermaid), followed by brief sections on:
  - the core entities and how they relate, including claim status history and the data behind outstanding liability exposure;
  - service boundaries: one service or several, and what drives that choice;
  - REST vs Kafka: topic names and event schemas;
  - the API contract and who owns it;
  - how the micro-frontends split the two user types, share components and state, and talk to the API;
  - how the async nature of claims shows up in both tiers;
  - what a claims officer sees to manage their workload, and how managers see outstanding claims and exposure.
- `NOTES.md`: one short entry per decision (context, decision, alternatives considered, trade-off) and a list of assumptions.
- `backend/src/main/resources/openapi/claims-api.yaml`: the API contract, written before the controllers that implement it.

## Step 3: Build

Build in the order of `docs/requirements.md`, backend first, aiming for production-quality code on a deliberately limited scope rather than wide coverage.

- **Backend:** Strict layering, with no class mixing API handling, business logic and data access. Input validation and one consistent error response format. Kafka producer and consumer for the events you designed. Seed data for a demo. Role-based access for claimant and staff; a simple, clearly documented authentication approach is enough.
- **Frontend:** The shell, the claimant micro-frontend and the staff micro-frontend. Typed against the OpenAPI contract. Predictable state management with clean data flow. A consistent theme with loading, empty and error states. Accessibility basics: labels, keyboard navigation, contrast.
- **Tests:**
  - Backend: unit tests for the claim status workflow and service rules, plus integration tests for the main endpoints and the Kafka event flow (Testcontainers is fine).
  - Frontend: component tests for key components, and one end-to-end test per key flow.
  - Name tests by behavior. Aim for about one focused test per stated behavior, and don't keep scratch scripts as permanent tests.
- **Commits:** Small, with messages that describe intent. Never "update" or "fix" alone, and never one large commit. The history should show the build in order: setup, design docs, domain model, each endpoint group, events, each micro-frontend, tests, docs.

If you find a bug, a performance concern, or behavior the requirements don't mention, don't fix or extend it unless the requested behavior can't work without it; note it in `docs/walkthrough.md` under "Next steps". Where a requirement is ambiguous, implement the reading the inputs most directly support, record the assumption in `NOTES.md`, and don't build for the other readings too.

When editing existing files, change only what needs to change; don't rewrite a whole file for a small edit.

## Step 4: Verify

Before you report anything as done, prove it:

- Clone the repository into a fresh directory and start it with the documented command, timing how long it takes.
- Run all backend and frontend test suites.
- Run the main demo flow end to end: a claimant reports an incident; staff pick up the claim and request more information; the claimant responds; staff approve or reject; the claimant sees the decision; the workload and exposure figures update.
- Check every item in `How_to_Structure_a_Take_Home_Assesment.md` (R1–R17) and every requirement in `docs/requirements.md`, marking each met, partly met or not met.
- Confirm that no secrets, build output or junk files are committed.

Report only what you can point to evidence for from this session. If tests fail, say so and include the output. If a step was skipped, say that.

## Step 5: Documentation

- `README.md` with exactly these sections: Overview, Architecture (link the diagram), Setup (one command, prerequisites, demo users), API Reference (link the OpenAPI file, list the key endpoints), Testing (what is covered and how to run it).
- `docs/walkthrough.md` for the reviewer and for my interview. It contains:
  - a 15–20 minute presentation outline (architecture, key decisions, demo script);
  - the hardest "why not X?" questions the panel is likely to ask, each with a short answer grounded in `NOTES.md`;
  - next steps in priority order.
- Optionally, `postman/` with a collection for manual testing.

## How to work

You are operating autonomously. The user is not watching in real time and cannot answer questions mid-task, so asking 'Want me to…?' or 'Shall I…?' will block the work. For reversible actions that follow from the original request, proceed without asking. Stop only for destructive actions or genuine scope changes the user must decide. Offering follow-ups after the task is done is fine; asking permission before doing the work is not.

Before ending your turn, check your last paragraph. If it is a plan, an analysis, a question, a list of next steps, or a promise about work you have not done ('I'll…', 'let me know when…'), do that work now with tool calls. That includes retrying after errors and gathering missing information yourself. Do not stop because the context or session is long. End your turn only when the task is complete or you are blocked on input only the user can provide.

Before running a command that changes system state (such as restarts, deletes, or config edits), check that the evidence actually supports that specific action. A signal that pattern-matches to a known failure may have a different cause.

The inputs set the scope, and the scope is the deliverable: don't quietly narrow, widen, or swap it. If a question comes up partway, first do everything that doesn't depend on the answer, then state the assumption you made. If one part is blocked, complete every other part in full and say exactly what you left out and why.

When you need several independent things, such as reading multiple files or running independent commands, request them together in one step rather than one at a time.

Before you start, say in a line what you're about to do; brief updates while you work help me follow along.

## Final message

When you finish, write a short summary for a reader who hasn't seen any of the work. Lead with the outcome: what runs, the one command to start it, and how long startup took. Then cover:

- What was built, mapped to the scoring areas.
- What was deliberately left out.
- Any failing tests or unverified items.
- The R1–R17 checklist result.
- The assumptions I most need to check before the interview.

Write in complete sentences, spell out terms, and don't use arrow chains or labels you made up while working.
