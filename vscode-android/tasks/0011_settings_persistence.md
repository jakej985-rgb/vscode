# Task 0011 — Settings Persistence

**Status:** ✅ Complete
**Priority:** P3 (Polish)
**Depends On:** Task 0005 ✅

---

## Goal
Allow users to persist editor preferences (Theme, Font Size, Word Wrap).

---

## Implementation Summary

### Backend (Node.js)
- **Store**: JSON file at `files/settings.json` (sibling to workspaces dir).
- **API**:
  - `GET /api/settings`: Returns JSON object (or empty).
  - `POST /api/settings`: Writes JSON object.

### Frontend (Browser)
- **UI**: Added "Settings" Sidebar view with form controls.
- **Logic**:
  - Loads settings on app start.
  - Applies settings to `monaco.editor.updateOptions` and `xterm.js` options.
  - Auto-saves changes to backend.

---

## Verification
- [x] Change font size to 20 -> Editor updates immediately.
- [x] Toggle Theme -> Editor and Terminal colors update.
- [x] Reload page -> Settings are preserved.

---

## Next Steps
Proceed to **Task 0012 (Git Integration)**.
