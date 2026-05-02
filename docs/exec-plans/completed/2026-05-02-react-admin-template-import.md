# Execution Plan: React Admin Template Import

Created: 2026-05-02
Status: Active
Author: agent

## Objective

Import `satnaing/shadcn-admin` into `dbflow-admin/` as a one-time React admin template source without changing the
existing Spring Boot backend or Thymeleaf management UI.

## Scope

**In scope:**

- `dbflow-admin/`
- `.gitignore`
- `docs/PLANS.md`
- This execution plan

**Out of scope:**

- `pom.xml`
- `src/main/java/**`
- `src/main/resources/templates/**`
- Spring Security, `/admin`, `/admin-next`, Maven frontend integration, package renaming, dependency pruning, and UI
  behavior changes

## Constraints

- Keep the existing Spring Boot and Thymeleaf management behavior untouched.
- Copy the upstream template without `.git/`, `.github/`, `node_modules/`, `dist/`, `.next/`, or other generated/vendor
  directories.
- Preserve upstream attribution with README context and a dedicated NOTICE.
- Treat this as scaffolding; TDD does not apply, so verification is structural and diff-based.
- Avoid committing because the worktree already contains user-staged root `PLANS.md` changes unrelated to this execution
  plan.

## Assumptions

- The upstream `main` branch is the intended source for this import.
- `satnaing/shadcn-admin` remains MIT licensed at import time.
- It is acceptable for the imported template to retain its original package name and demo branding until Prompt 1.2.

## Acceptance Criteria

- [ ] AC-1: `dbflow-admin/package.json` exists.
- [ ] AC-2: `dbflow-admin/src/main.tsx` exists.
- [ ] AC-3: `dbflow-admin/NOTICE.md` records upstream project, repository URL, import date, and MIT license.
- [ ] AC-4: `dbflow-admin/README.md` starts with a Refinex-DBFlow one-time-import note.
- [ ] AC-5: `.gitignore` ignores `dbflow-admin/node_modules/`, `dbflow-admin/dist/`, `dbflow-admin/.vite/`,
  `dbflow-admin/playwright-report/`, and `dbflow-admin/test-results/`.
- [ ] AC-6: `pom.xml`, `src/main/java/**`, and `src/main/resources/templates/**` are not modified.

## Risk Notes

| Risk                                                         | Likelihood | Mitigation                                                                   |
|--------------------------------------------------------------|------------|------------------------------------------------------------------------------|
| Upstream repository contains generated or vendor directories | Low        | Use archive/rsync exclusion and verify forbidden paths are absent            |
| Scope drift into Prompt 1.2                                  | Medium     | Only add import attribution and ignore rules; do not rename package or brand |
| Existing staged user changes get mixed into commits          | Medium     | Do not commit during this task                                               |

## Implementation Steps

### Step 1: Import upstream template

**Files:** `dbflow-admin/**`
**Verification:** `test -f dbflow-admin/package.json && test -f dbflow-admin/src/main.tsx`

Status: ✅ Done
Evidence: `dbflow-admin/package.json` and `dbflow-admin/src/main.tsx` exist; forbidden import directories scan returned
no matches.
Deviations:

### Step 2: Add attribution and ignore rules

**Files:** `dbflow-admin/README.md`, `dbflow-admin/NOTICE.md`, `.gitignore`
**Verification:** inspect file heads and grep expected ignore entries

Status: ✅ Done
Evidence: `dbflow-admin/README.md` starts with a Refinex-DBFlow import note; `dbflow-admin/NOTICE.md` records upstream
project, URL, branch, commit, date, and MIT license; `.gitignore` contains all requested `dbflow-admin/` ignore entries.
Deviations:

### Step 3: Verify scope and control plane

**Files:** repository state
**Verification:** structural checks, forbidden-path checks, `python3 scripts/check_harness.py`, `git diff --check`, and
diff scope inspection

Status: ✅ Done
Evidence: `python3 scripts/check_harness.py` passed; `git diff --check` passed; forbidden directory scan returned no
matches; restricted backend path diff checks returned no matches.
Deviations:

## Progress Log

| Step      | Status | Evidence                                                                                                 | Notes                                                                             |
|-----------|--------|----------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| Preflight | ✅      | `python3 scripts/check_harness.py`; `./mvnw test`                                                        | Harness passed; Maven baseline passed 191 tests, 0 failures, 0 errors, 10 skipped |
| 1         | ✅      | `test -f dbflow-admin/package.json`; `test -f dbflow-admin/src/main.tsx`; forbidden directory scan empty |                                                                                   |
| 2         | ✅      | README/NOTICE inspected; `.gitignore` entries matched                                                    |                                                                                   |
| 3         | ✅      | Harness, diff hygiene, forbidden directory, and restricted path checks passed                            |                                                                                   |

## Decision Log

| Decision                                   | Context                                        | Alternatives Considered                | Rationale                                                        |
|--------------------------------------------|------------------------------------------------|----------------------------------------|------------------------------------------------------------------|
| Use structural verification instead of TDD | Template import has no runtime behavior change | Add tests for file existence           | Shell/file checks prove the acceptance criteria with lower noise |
| Do not commit plan first                   | User-staged root `PLANS.md` already exists     | Commit only plan with careful pathspec | Avoid touching user-staged work during this narrow task          |

## Completion Summary

Completed: 2026-05-02
Duration: 3 steps
All acceptance criteria: PASS

Summary:

- Imported `satnaing/shadcn-admin` from `main` commit `a6352e7df0de652e4349f6bf53ca246de6ff013f` into `dbflow-admin/`.
- Added Refinex-DBFlow import context to `dbflow-admin/README.md`.
- Added `dbflow-admin/NOTICE.md` with upstream project, repository URL, branch, commit, import date, and MIT license.
- Added requested `dbflow-admin/` generated-directory ignores to the root `.gitignore`.
- Verified `pom.xml`, `src/main/java/**`, and `src/main/resources/templates/**` were not modified.

Verification:

- `./mvnw test` passed before implementation: 191 tests, 0 failures, 0 errors, 10 skipped.
- `python3 scripts/check_harness.py` passed after implementation.
- `git diff --check` passed after implementation.
- Structural checks confirmed `dbflow-admin/package.json`, `dbflow-admin/src/main.tsx`, and `dbflow-admin/NOTICE.md`
  exist.
- Forbidden directory scan for `.git`, `.github`, `node_modules`, `dist`, and `.next` under `dbflow-admin/` returned no
  matches.
