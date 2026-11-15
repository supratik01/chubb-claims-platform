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
