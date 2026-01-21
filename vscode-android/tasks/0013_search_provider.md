# Task 0013 — Search Provider

**Status:** ✅ Complete
**Priority:** P2 (Feature)
**Depends On:** Task 0005 ✅

---

## Goal
Allow users to search for text within files across the entire workspace.

---

## Implementation Summary

### Backend (Node.js)
- **Endpoint**: `GET /api/search?ws=id&q=text`
- **Logic**: 
  - Recursively scans workspace directories.
  - Skips `.git`, `node_modules`, `dist`, `build`.
  - Reads text files (up to 500KB) and checks content.
  - Returns Matches: `[{ file, line, text }]`.
  - Limits: Max 200 results.

### Frontend (Browser)
- **UI**: Added "Search" sidebar view.
- **Action**: Input box triggers API search.
- **Results**: Visual list of matches. Clicking opens file and scrolls to line using `monaco.editor.revealLineInCenter`.

---

## Verification
- [x] Search for known string -> Returns correct file and line.
- [x] Click result -> Editor opens and navigates to line.
- [x] Search respects ignores (no hits from node_modules).

---

## Next Steps
Proceed to **Task 0014 (Plugin API)** or **Task 0015 (Documentation)**.
