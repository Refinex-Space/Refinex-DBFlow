# Execution Plan: React Admin Brand Baseline

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Rename the imported React admin template package and visible baseline branding to DBFlow Admin while preserving upstream
attribution and demo pages for later cleanup.

## Scope

**In scope:**

- `dbflow-admin/package.json`
- `dbflow-admin/index.html`
- Page-visible template branding under `dbflow-admin/src/**`
- `docs/PLANS.md`
- This execution plan

**Out of scope:**

- Removing Clerk routes, dependencies, or demo pages
- Productizing navigation, authentication, API clients, or `/admin-next`
- Changing Spring Boot backend code, Maven configuration, or Thymeleaf templates
- Removing upstream references in `dbflow-admin/README.md`, `dbflow-admin/NOTICE.md`, or `dbflow-admin/LICENSE`

## Constraints

- Preserve upstream attribution and license references.
- Keep demo pages for later prompts; only rename visible product/admin labels where safe.
- Use the backend version string `0.1.0-SNAPSHOT` in `dbflow-admin/package.json`.
- Do not change `pom.xml`, `src/main/java/**`, or `src/main/resources/templates/**`.
- TDD does not fit this branding/package metadata task; verification is install/build/search/diff based.

## Assumptions

- `0.1.0-SNAPSHOT` is acceptable as an npm prerelease version.
- Clerk-specific instructional copy remains in demo Clerk routes until the planned Clerk removal prompt.
- Shadcn UI library/tooling references should remain when they describe technology rather than product branding.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/package.json` has `name: "refinex-dbflow-admin"`, `private: true`, and
  `version: "0.1.0-SNAPSHOT"`.
- [x] AC-2: Browser metadata in `dbflow-admin/index.html` uses `DBFlow Admin`, `Refinex-DBFlow`, and `MCP SQL Gateway`
  instead of `Shadcn Admin` product metadata.
- [x] AC-3: Shared app title/sidebar/profile visible shell labels use `Refinex-DBFlow`, `DBFlow Admin`, and
  `MCP SQL Gateway`.
- [x] AC-4: Upstream attribution remains in README/NOTICE/License.
- [x] AC-5: `pnpm --dir dbflow-admin install` completes successfully.
- [x] AC-6: `pnpm --dir dbflow-admin build` completes successfully.
- [x] AC-7: `pom.xml`, `src/main/java/**`, and `src/main/resources/templates/**` are not modified.

## Risk Notes

| Risk                                                                 | Likelihood | Mitigation                                                                            |
|----------------------------------------------------------------------|------------|---------------------------------------------------------------------------------------|
| Generated TanStack route tree contains Clerk identifiers             | High       | Do not edit generated route names in this prompt; Clerk removal belongs to Prompt 2.1 |
| Search finds upstream attribution and technology references          | High       | Distinguish preserved attribution/tech references from page-visible DBFlow branding   |
| `pnpm install` mutates the lockfile because package metadata changed | Medium     | Accept lockfile metadata update if produced by pnpm install and verify build          |

## Implementation Steps

### Step 1: Rename package metadata

**Files:** `dbflow-admin/package.json`, `dbflow-admin/pnpm-lock.yaml` if install updates importer metadata
**Verification:** inspect package metadata and run `pnpm --dir dbflow-admin install`

Status: ✅ Completed
Evidence: `dbflow-admin/package.json` now uses `refinex-dbflow-admin`, `private: true`, and `0.1.0-SNAPSHOT`;
`pnpm --dir dbflow-admin install` completed successfully.
Deviations: None.

### Step 2: Replace visible product branding

**Files:** `dbflow-admin/index.html`, selected files under `dbflow-admin/src/**`
**Verification:** search for requested template branding and confirm remaining matches are upstream attribution,
technology references, tests, generated Clerk/demo routes, or deferred demo copy

Status: ✅ Completed
Evidence: `rg 'DBFlow Admin|Refinex-DBFlow|MCP SQL Gateway' dbflow-admin/index.html dbflow-admin/src` shows browser
metadata, app title, sidebar teams, profile dropdown, auth shell, sign-in page, Clerk demo auth shell title, and logo
title updated.
Deviations: Clerk-specific demo copy and dependencies remain by scope; README/NOTICE/License upstream attribution
remains by requirement.

### Step 3: Build and scope verification

**Files:** repository state
**Verification:** `pnpm --dir dbflow-admin build`, `python3 scripts/check_harness.py`, `git diff --check`, restricted
backend path diff checks

Status: ✅ Completed
Evidence: `pnpm --dir dbflow-admin build` completed successfully; `python3 scripts/check_harness.py` passed;
`git diff --check` passed; restricted backend/Thymeleaf path diff checks returned no modified files.
Deviations: `vite build` regenerated `dbflow-admin/src/routeTree.gen.ts` formatting through TanStack Router's generated
route tree. `dbflow-admin/src/styles/index.css` received a minimal Tailwind v4 build fix for the existing `faded-bottom`
utility because the template's `@apply` expression failed the required build gate.

## Progress Log

| Step      | Status | Evidence                                                                                                                | Notes                                                                             |
|-----------|--------|-------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|
| Preflight | ✅      | `python3 scripts/check_harness.py`; `./mvnw test`                                                                       | Harness passed; Maven baseline passed 191 tests, 0 failures, 0 errors, 10 skipped |
| 1         | ✅      | `dbflow-admin/package.json`; `pnpm --dir dbflow-admin install`                                                          | Package metadata renamed to DBFlow Admin package baseline                         |
| 2         | ✅      | `rg 'DBFlow Admin                                                                                                       | Refinex-DBFlow                                                                    |MCP SQL Gateway' dbflow-admin/index.html dbflow-admin/src` | Visible app shell/auth branding updated; upstream and demo references preserved |
| 3         | ✅      | `pnpm --dir dbflow-admin build`; `python3 scripts/check_harness.py`; `git diff --check`; restricted backend diff checks | Build passed after minimal CSS build fix; no backend/Thymeleaf changes            |

## Decision Log

| Decision                                   | Context                                                                      | Alternatives Considered                       | Rationale                                                                                                          |
|--------------------------------------------|------------------------------------------------------------------------------|-----------------------------------------------|--------------------------------------------------------------------------------------------------------------------|
| Preserve Clerk route copy and dependencies | User explicitly asked to keep demo pages and later cleanup will remove Clerk | Remove or rewrite all Clerk strings now       | Avoid scope drift into Prompt 2.1                                                                                  |
| Preserve README/NOTICE upstream references | User explicitly said not to delete upstream License/NOTICE explanation       | Rewrite all `satnaing`/`shadcn-admin` strings | Attribution must remain auditable                                                                                  |
| Use `0.1.0-SNAPSHOT` frontend version      | User allowed backend-aligned `0.1.0-SNAPSHOT` or npm-friendly `0.1.0`        | Use `0.1.0`                                   | Keeping the backend-aligned version makes the admin package stage visibly tied to the Spring Boot project baseline |

## Completion Summary

Renamed the imported React admin package to `refinex-dbflow-admin`, updated visible baseline branding to `DBFlow Admin`,
`Refinex-DBFlow`, and `MCP SQL Gateway`, preserved upstream attribution and demo pages, and verified install/build plus
Harness and diff gates. No Spring Boot backend, Maven, or Thymeleaf template paths were modified.
