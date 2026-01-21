# CodePocket Local — Project Scope & Roadmap

**Goal:** Create a "fully operational" v1.0 local code editor for Android that feels like a real editor (VS Code) on a phone/tablet.

---

## Core Architecture

The "Electron Replacement" for Android:

1.  **Kotlin App (Shell)**
    *   **WebView UI**: Renders the editor interface.
    *   **ForegroundService**: Keeps the Node.js runtime alive.
    *   **SAF Integration**: Bridges Android's Scoped Storage with the local filesystem via a "Mirror & Sync" model.
    *   **Workspace Manager**: UI for managing projects.

2.  **Embedded Node Runtime**
    *   **Engine**: `nodejs-mobile-android`.
    *   **Lifecycle**: Single instance per app process. Logic restarts happen *inside* the runtime without exiting the engine.
    *   **Role**: Host the VS Code backend and run extensions.

3.  **VS Code Web Host**
    *   **Base**: OpenVSCode Server / Monaco Editor (MVP).
    *   **Binding**: `127.0.0.1:<port>` (Loopback only).
    *   **Security**: Token-based access to prevent external access.

4.  **Filesystem Model**
    *   **Mirror**: SAF Folder (Slow) → App Sandbox (Fast). Node.js works on the sandbox copy.
    *   **Sync**: Incremental export from Sandbox → SAF Folder.

---

## Roadmap

### Phase 1: Infrastructure (MVP) [COMPLETE]
*   ✅ **0001** Local WebView Shell (File chooser, downloads)
*   ✅ **0002** Embed Node Runtime (Mock mode implemented)
*   ✅ **0003** Local Host Serving UI (Monaco Editor on localhost)
*   ✅ **0004** Workspace SAF Mirror (Import/Mirror mechanism)

### Phase 2: Core Functionality [CURRENT]
*   ⏳ **0005** Connect Editor to Mirrored FS (Real file editing)
*   **0006** Sync Engine (Mirror → SAF)

### Phase 3: Security & Extensions
*   **0007** Security Hardening (Loopback binding, tokens)
*   **0008** Extensions (Open VSX integration, guardrails)

### Phase 4: Developer Tools
*   **0009** Terminal / Tasks Runner
*   **0010** Git Basics (Status/Diff/Commit)
*   **0011** Language Intelligence Packs (LSP/WASM)

### Phase 5: Polish & Release
*   **0012** Mobile UX Polish (Touch targets, tablet layout)
*   **0013** Performance & Stability (Startup profiling, memory)
*   **0014** Automated Testing (E2E harness)
*   **0015** Release Pipeline (CI/CD, Signing)

---

## Defined Limits (v1.0)
*   **Single Runtime**: No multi-process extension host (Android limitations).
*   **Mirroring**: Large projects (>500MB) may need optimizations or partial mirroring.
*   **Extensions**: Web-compatible extensions preferred; native binary extensions are experimental/unsupported.
