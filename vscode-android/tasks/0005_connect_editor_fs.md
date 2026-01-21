# Task 0005 — Connect Editor to Real Filesystem

**Status:** ✅ Complete
**Priority:** P0 (Functional Core)
**Depends On:** Task 0004 ✅

---

## Goal

Connect the Monaco Editor UI (`index.html`) to the backend file system API (`main.js`) so users can actually edit files in their mirrored workspaces.

---

## Code Changes

### `main.js` (Node Backend)
- Implemented `/api/workspaces` to list available workspace folders.
- Implemented `/api/files?ws=ID` for recursive file listing.
- Implemented `/api/file?ws=ID&path=...` for Read (GET) and Write (POST).
- Security: Restricted access to `files/workspaces/{ID}` root.

### `index.html` (Frontend)
- Replaced sample data with real API calls.
- Added `loadWorkspace(id)` logic.
- Added basic tab management connected to file content.
- Added `Ctrl+S` -> POST `/api/file` save handler.

### Android Integration
- **WebViewActivity**: Accepts `EXTRA_WORKSPACE_ID` intent extra to append `?ws=ID` to localhost URL.
- **WorkspaceManagerActivity**: Passes the workspace ID when launching editor.

---

## Verification

### Build Status
```
BUILD SUCCESSFUL in 32s
```

### Manual Validation
- [x] Launching workspace loads real file tree.
- [x] Opening file fetches content from Node.
- [x] Saving file writes to `filesDir/workspaces/{ID}`.
- [x] "Open Folder" in editor lists workspaces if none selected.

---

## Notes
- Monaco Editor assets are still loaded from CDN (40MB download skipped for MVP speed). Offline bundling deferred to polish phase.
- `WORKSPACES_ROOT` in Node is correctly mapped to Android's `filesDir/workspaces`.

---

## Next Steps

Proceed to **Task 0006 (Sync Engine)** to allow exporting these edits back to SAF.
