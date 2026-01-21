# Task 0009 — Integrated Terminal

**Status:** ✅ Complete (MVP)
**Priority:** P2 (Developer Experience)
**Depends On:** Task 0002 ✅

---

## Goal
Provide a built-in terminal shell to run commands within the workspace environment.

---

## Implementation Summary

### Backend (Node.js)
- **Spawn**: Uses `child_process.spawn('/system/bin/sh')`.
- **Management**: Tracks sessions in memory (`terminals` object).
- **Communication**:
  - `POST /api/terminals`: Start session.
  - `POST /api/terminals/:id/input`: Write to stdin.
  - `GET /api/terminals/:id/output`: Consume stdout/stderr buffer.

### Frontend (Browser)
- **UI**: Added toggleable bottom panel (`Ctrl+Backtick`).
- **xterm.js**: Renders the terminal interface.
- **Polling**: Fetches output every 200ms (WebSocket deferred).

### Limitations (v1)
- **No PTY**: Apps expecting a TTY (vim, nano, colors) may misbehave or have formatting issues.
- **Latency**: Polling introduces slight delay.
- **Shell**: Limited to Android system shell (sh).

---

## Verification
- [x] Toggle terminal with `Ctrl+` ` ` ` or status bar click.
- [x] Type `pwd` -> Returns workspace path.
- [x] Type `ls` -> Lists files.

---

## Next Steps
- Upgrade to WebSocket for real-time performance.
- Investigate `node-pty` native build for Android (complex).
- Proceed to **Task 0010 (Branding & Polish)**.
