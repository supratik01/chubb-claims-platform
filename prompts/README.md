## How the master prompt was built (before the build session)

To fit the 3–5 hour time box and stay within my Claude usage limits, I split the work into two stages: a planning stage where I worked with Claude to turn the inputs into one master prompt, and a build stage where an
agent builds from that prompt. Steps:

1. **Understood the brief.** Gave Claude the assessment brief and asked for a plain-English
   breakdown of what to build. Reviewed it and kept it as a reference document.
2. **Chose the prompting approach.** Used my own fable5-prompting skill, which I had updated
   that morning with Anthropic's latest Fable 5.1 guidance, to write the master prompt.
3. **First draft.** The first version asked the build agent to analyse the structure video
   itself (scene detection, frame extraction, audio transcription).
4. **Override.** I decided video analysis didn't belong in the build run: it adds time and
   risk to every build attempt. I ran it once, separately, reviewed the output, and
   condensed it myself into `How_to_Structure_a_Take_Home_Assesment.md` (R1–R17), which
   the build agent now uses as its checklist.
5. **Re-prioritised with the scoring rubric.** Added the drive slides as an input. Slide 3
   is the scoring rubric. It showed that micro-frontends (15 pts) and AI collaboration
   (25 pts) matter more than feature count (12 pts), so the prompt now fixes a
   micro-frontend design and requires the prompts folder and this journal.
6. **Tailored the final prompt myself.** I edited the generated master prompt to fit my
   needs.
7. **Kept myself in the loop.** The prompt leaves a "Candidate review" line on every entry
   so that accept / challenge / override decisions stay mine.