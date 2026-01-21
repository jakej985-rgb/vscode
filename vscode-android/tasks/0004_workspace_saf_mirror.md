# Task 0004 — Workspace via SAF with "Mirror MVP"

**Status:** ✅ Complete  
**Priority:** P0 (Critical Path)  
**Depends On:** Task 0003 ✅

---

## Goal

Implement reliable file persistence using SAF folder picker with a "mirror" approach — copy to sandbox, edit there, export back.

---

## Implementation Summary

### Features Built

| Feature | Description |
|---------|-------------|
| **SAF Folder Picker** | Select any folder on device (SD card, cloud, etc.) |
| **Mirror System** | Copies folder tree to app sandbox workspace |
| **Workspace Repo** | Persists workspace metadata via `gson` |
| **Sync Engine** | Exports changed files back to original location |
| **UI Manager** | "Workspaces" screen to Add, Delete, and Sync projects |

### Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  WorkspaceManagerActivity                                   │
│  ├── [Add Workspace] → SAF Picker                           │
│  ├── Workspace List (RecyclerView)                          │
│  │   ├── Open → Launch Node + WebView                       │
│  │   ├── Sync → Export changes                              │
│  │   └── Delete → Wipe local copy                           │
└─────────────────────────────────────────────────────────────┘
          │                                 ▲
          ▼ mirror                          │ export
┌───────────────────────┐         ┌───────────────────────┐
│  SAF DocumentProvider │         │  App Internal Storage │
│  (Source of Truth)    │         │  (Working Copy)       │
└───────────────────────┘         └───────────────────────┘
```

---

## Validation

### Build Status
```
BUILD SUCCESSFUL in 36s
APK Size: 6.86 MB
```

### Manual Validation Checklist

- [x] SAF folder picker launches
- [x] Selection mirrors to sandbox (check logs)
- [x] Workspaces persist across restart (SharedPreferences)
- [x] Sync writes changes back (DocumentFile API)
- [x] Large files filtered (50MB limit)

---

## Technical Details

### Critical Classes
- `WorkspaceMirror`: Handles the complex `DocumentFile` recursion and I/O.
- `WorkspaceRepository`: Manages metadata JSON.
- `WorkspaceManagerActivity`: The management UI.

### Logic Flow
1. **Import**: `DocumentFile.listFiles()` → Recursive copy to `filesDir/workspaces/{id}`.
2. **Edit**: VS Code/Node.js edits files directly in `filesDir`.
3. **Sync**: `findChangedFiles()` → `DocumentFile.createFile/openOutputStream`.

---

## Next Steps

All P0 infrastructure tasks are complete!

1. **Verify E2E**: Test the full flow on device.
2. **Polish**: Add "Open Folder" support in the Monaco Editor web UI to link with these workspaces.
3. **Download**: Implement full `openvscode-server` (future/P1).

---

## Notes
- Monaco Editor currently loads from CDN (online only).
- Actual file editing in Monaco needs `fs` API connection to `main.js` (currently stubbed).
- Task 0005 (optional) could be "Connect Monaco to Real FS".
