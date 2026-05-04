# Agent-Ready Repository Documentation — Design

**Date:** 2026-04-27
**Status:** Approved

## Overview

Enhance the repository for supervised agent-assisted development by adding an `AGENTS.md` guide and configuring tool permissions in `.claude/settings.json`. Goal: give agents enough context to be productive without reinventing what's already documented, while minimizing permission friction.

## Scope

1. Create `AGENTS.md` — agent-specific context doc
2. Update `.claude/settings.json` — add tool permissions for common operations
3. Update `README.md` — remove stale sync module reference

## Out of Scope

- Modifying `CLAUDE.md` (existing architecture spec is sufficient)
- Creating `CONTRIBUTING.md` or expanding beyond agent-specific docs
- Changing git workflow (existing patterns preserved)

---

## AGENTS.md

**Location:** `AGENTS.md` (root level, sibling to `CLAUDE.md`)

**Purpose:** One-page reference for agents working in this repo. Not exhaustive — agents should still use skills (especially superpowers) and reference CLAUDE.md for architecture.

**Content sections:**

### Project Overview
- Kotlin Multiplatform (Android + iOS)
- Clean Architecture with feature folders
- Compose Multiplatform UI

### Implemented Features
- Auth (Google Sign-In via Firebase)
- Todos (Firestore persistence)
- Habits (Room KMP local persistence)
- Notifications (local scheduling, multi-platform abstracted)

### Planned (agents should not implement)
- Pomodoro timer
- Agenda (calendar overview)

### Build & Test Commands

**Android:**
```bash
./gradlew :composeApp:assembleDebug
```

**iOS:**
Open `iosApp/iosApp.xcworkspace` in Xcode.

**Verification triggers** (significant changes):
- New feature implementation
- UI or navigation changes
- Dependency changes
- Any change affecting both platforms

**Verification approach:** Run the relevant platform build, confirm it completes without errors.

### What to Avoid
- Do not use `java.*` imports in `commonMain`
- Do not use `java.util.Date` — use `kotlinx-datetime`
- Do not commit directly to `main` — use feature branches
- Do not force-push to `main` or any shared branch
- Do not modify `sync/` module (stale, to be removed) — actually removed already
- Do not create specs/plans outside `docs/superpowers/` — superpowers skill handles that workflow

### KMP Patterns
- `expect`/`actual` for platform-specific builders (Room, DataStore)
- Domain interfaces in `commonMain`, implementations in platform-specific or `commonMain` for shared logic
- Koin for DI — use `singleOf`, `factoryOf`, `viewModelOf`

### Navigation
- Type-safe routes via `NavRoute` sealed class + kotlinx-serialization
- Bottom navigation driven by feature flags in `FeatureFlagManager`

---

## Tool Permissions (`.claude/settings.json`)

Add permissions for common operations to reduce agent friction:

```json
{
  "permissions": {
    "allow": [
      { "tool": "Bash", "command": "./gradlew :composeApp:assembleDebug" },
      { "tool": "Bash", "command": "./gradlew :composeApp:build" },
      { "tool": "Bash", "command": "git branch", "reason": "View current branches" },
      { "tool": "Bash", "command": "git status", "reason": "Check working tree state" },
      { "tool": "Bash", "command": "git diff", "reason": "Review changes before commit" },
      { "tool": "Bash", "command": "git log", "reason": "Check commit history" },
      { "tool": "Bash", "command": "git checkout -b", "reason": "Create feature branches" },
      { "tool": "Bash", "command": "git add", "reason": "Stage files for commit" },
      { "tool": "Bash", "command": "git commit", "reason": "Commit changes" }
    ]
  }
}
```

Existing plugins and settings preserved.

---

## README.md Update

Remove sync module reference from features list:

```diff
 ### Implemented:

 - **Auth** — Google Sign-In with persistent session support...
 - **Todo** — Full CRUD UI with **Cloud Firestore**...
 - **Habits** — ...
 - **Notifications** — ...

-### Planned:
-
-- **Pomodoro** — Simple timer for deep focus.
-- **Agenda** — Calendar-based overview combining Todos and Habits.
```

Keep Pomodoro and Agenda in "Planned" section — they are legitimate roadmap items, just not implemented.

---

## Files to Create/Modify

| File | Action |
|------|--------|
| `AGENTS.md` | Create |
| `.claude/settings.json` | Modify — add permissions |
| `README.md` | Modify — remove sync reference |
| `docs/superpowers/specs/2026-04-27-agent-docs-design.md` | Create (this doc) |

---

## Self-Review

- No placeholder text or TODOs
- Scope contained to single implementation plan
- Build commands verified (from existing README)
- Git permissions scoped to non-destructive operations
- Avoid section covers stale sync module (now removed)