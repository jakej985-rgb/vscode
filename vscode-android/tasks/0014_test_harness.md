# Task 0014 — Automated Testing

**Status:** ⏳ Pending
**Priority:** P3 (Quality)
**Depends On:** Task 0005

---

## Goal
Automate regression testing for critical workflows.

---

## Acceptance Criteria
- [ ] Unit tests for Workspace Mirror/Sync logic.
- [ ] Instrumentation tests (Espresso/UiAutomator):
    - Launch App -> Open Workspace -> Edit File -> Save -> Relaunch -> Verify.
- [ ] "Golden Path" E2E script documented.

---

## Technical Approach
- JUnit 5 / Espresso.
- Mock Node service for UI tests.
