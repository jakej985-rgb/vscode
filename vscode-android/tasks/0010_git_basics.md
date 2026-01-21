# Task 0010 — Git Basics

**Status:** ⏳ Pending
**Priority:** P2 (Feature)
**Depends On:** Task 0005

---

## Goal
Enable basic Git workflows (status, diff, commit) within the mirrored workspace.

---

## Acceptance Criteria
- [ ] Git capability available in host/environment.
    - Option A: `isomorphic-git` (Pure JS, runs in Node).
    - Option B: Native git binary (complex to bundle).
- [ ] Source Control view shows changes (git status).
- [ ] Diff view works.
- [ ] Commit action works.
- [ ] Operations restricted to workspace root.

---

## Technical Approach
- **Recommendation**: `isomorphic-git` via a VS Code extension or baked into the host.
- It is pure JavaScript and works well in Node.js environments without needing a native binary.
