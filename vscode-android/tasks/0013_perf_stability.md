# Task 0013 — Performance & Stability

**Status:** ⏳ Pending
**Priority:** P3 (Quality)
**Depends On:** Task 0005

---

## Goal
Ensure the app runs reliably on mid-tier Android devices without crashes or prohibitive lag.

---

## Acceptance Criteria
- [ ] Startup time (Launch → Editor Interactive) < 5 seconds.
- [ ] Memory usage profiled and capped (large files don't OOM).
- [ ] Background/Foreground transitions stable (service doesn't die unexpectedly).
- [ ] "Safe Mode" implemented (launch without extensions/workspace).

---

## Technical Approach
- Android Profiler sessions.
- WebView memory management callbacks.
- Node.js heap limits flags.
