# AI working journal

A running record of the AI-assisted build of the Chubb APAC claims platform, written as the work happened.

- **AI used:** Claude (Claude Code, model `claude-fable-5-2`), working autonomously from the prompts in `prompts/`.
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

---

## Entry 03 — 2026-11-15 14:20 IST — Upfront design

**Instruction:** Step 2: write `docs/requirements.md`, `docs/architecture.md`, `NOTES.md` and the OpenAPI contract
before any feature code.

**Produced:** The four files. 43 requirements traced to their source (31 must-have, 8 deferred with reasons, 4
delivery items that are simply followed). 19 decisions, 9 assumptions and 6 recorded conflicts in `NOTES.md`. The
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

---
