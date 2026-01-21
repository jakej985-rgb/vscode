# Task 0012 — Mobile UX Polish

**Status:** ⏳ Pending
**Priority:** P3 (Polish)
**Depends On:** Task 0003

---

## Goal
Make the editor feel designed for mobile/tablet, rather than just "a webpage in a box".

---

## Acceptance Criteria
- [ ] Touch targets resized (larger icons/menus).
- [ ] Gesture-friendly explorer (swipe actions?).
- [ ] Split view support for tablets/foldables.
- [ ] Virtual keyboard accessory view (Tab, Esc, Ctrl keys).
- [ ] External keyboard shortcuts map correctly.

---

## Technical Approach
- Custom CSS injection into the WebView.
- `vscode-custom-css` extension or similar mechanism.
- Android-side helper view for the virtual keyboard accessory bar.
