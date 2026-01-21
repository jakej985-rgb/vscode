# Task 0011 — Language Intelligence Packs

**Status:** ⏳ Pending
**Priority:** P3 (Enhancement)
**Depends On:** Task 0005

---

## Goal
Provide intelligent code completion and diagnostics for popular languages.

---

## Acceptance Criteria
- [ ] Base web features enabled (TextMate grammars).
- [ ] TypeScript/JavaScript language support (built-in).
- [ ] Additional packs available as optional installs (to save APK size).
- [ ] Python Pack (Pyright or similar via fast-python-parser).
- [ ] C/C++ Pack (WASM-based preferred).

---

## Technical Approach
- Leverage VS Code's "Any Web" extension capabilities (WASM-based Language Servers).
- Keep core APK small; download LSPs on demand if possible.
