# Task 0008 — Extensions (Open VSX)

**Status:** ⏳ Pending
**Priority:** P2 (Feature)
**Depends On:** Task 0005

---

## Goal
Enable extension installation via Open VSX Registry, with compatibility filters and security guardrails.

---

## Acceptance Criteria
- [ ] Configure product.json/backend to use Open VSX (https://open-vsx.org).
- [ ] Add compatibility filter middleware:
    - Prefer "web" extensions.
    - Warn/Block known incompatible extensions (native binaries).
- [ ] UI for searching and installing extensions works.
- [ ] Extensions persist across app restarts.
- [ ] "Trust" dialog before ensuring extension can run.

---

## Technical Approach
- VS Code compatible `product.json` configuration.
- Custom service to proxy/filter Open VSX requests if needed, or client-side filtering.
- Sandbox extension host (webworker or Node.js process).
