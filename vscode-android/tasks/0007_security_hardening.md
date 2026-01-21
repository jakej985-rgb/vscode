# Task 0007 — Security Boundary & Hardening

**Status:** ✅ Complete
**Priority:** P1 (Security)
**Depends On:** Task 0003 ✅

---

## Goal
Secure the local web host to prevent malicious apps or network attackers from accessing the user's code.

---

## Implementation Summary

### Token Authentication
- **NodeService**: Generates a random UUID on startup and saves it to `.auth_token` in the project root.
- **main.js**: Reads `.auth_token` on startup. Rejects any `/api/` request that doesn't provide the token via header (`Authorization: Bearer <token>`) or query parameter (`?token=<token>`).
- **WebViewActivity**: Reads `.auth_token` from internal storage and appends `?token=XYZ` to the WebView URL.
- **index.html**: Extracts the token from the URL and includes it in the `Authorization` header for all API calls.

### Loopback Binding
- Host is hardcoded to bind only to `127.0.0.1` (implemented in Task 0002/0003).

---

## Verification
- [x] Node service generates `.auth_token`.
- [x] Frontend successfully loads with token.
- [x] Direct curl to `/api/workspaces` without token returns 403 Forbidden.

---

## Next Steps
Proceed to **Task 0008 (Extensions)**.
