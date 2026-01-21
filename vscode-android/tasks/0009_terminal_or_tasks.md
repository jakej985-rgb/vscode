# Task 0009 — Terminal Strategy

**Status:** ⏳ Pending
**Priority:** P2 (Feature)
**Depends On:** Task 0005

---

## Goal
Provide a way to run commands (build, test, scripts) within the editor.

---

## Acceptance Criteria
- [ ] Decision made: Real PTY vs Tasks Runner.
- [ ] Lane B (Tasks Runner) MVP:
    - Parse `package.json` scripts.
    - UI button to run a script.
    - Output panel shows stdout/stderr.
- [ ] OR Lane A (Real Terminal) [Harder]:
    - PTY implementation (node-pty-prebuilt-multiarch?).
    - xterm.js integration in editor.

---

## Technical Approach
- Start with **Lane B (Tasks Runner)** for v1.
- Use Node.js `child_process.spawn`.
- Stream output to a custom "Output" channel in Monaco.
