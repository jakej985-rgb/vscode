# Task 0008 — Extensions Marketplace (Open VSX)

**Status:** ✅ Complete (MVP)
**Priority:** P2 (Feature)
**Depends On:** Task 0003 ✅

---

## Goal
Allow users to find and install extensions (specifically Themes) for the editor.

---

## Implementation Summary

### Extensions UI
- Added **Activity Bar** component to search extensions.
- Added **Search View** querying `open-vsx.org` API.
- Displays extension icon, name, publisher, and description.

### Registry Integration
- **Search**: `GET https://open-vsx.org/api/-/search`
- **CORS**: Direct browser fetch works (Open VSX allows CORS).

### Limitations (v1)
- **Install**: Currently a mock action. Downloading and applying themes requires VSIX extraction (ZIP) which is pending `JSZip` integration.
- **Runtime**: Only "web extensions" (themes, snippets) are theoretically supported by the Monaco host. Complex extensions requiring VS Code Node API will NOT work.

---

## Verification
- [x] Clicking "Extensions" icon switches sidebar.
- [x] Typing "theme" lists themes from Open VSX.
- [x] UI handles loading and error states.

---

## Next Steps
- Implement VSIX downloader and unzipper.
- Implement Theme Service to apply downloaded JSON themes.
- Proceed to **Task 0009 (Terminal)**.
