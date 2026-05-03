# 2026-05-03 React Admin Cutover Docs

## Objective

Update user-visible documentation after the React `/admin` cutover and remove the obsolete root-level React admin
planning document.

## Scope

- Update README, admin guide, deployment guide, architecture, observability, and Harness plan index.
- Document that `/login` and `/admin` are the current management entries.
- Document `dbflow-admin/` and the React admin stack: React, Vite, TypeScript, shadcn/ui, Tailwind, and TanStack.
- Document `./mvnw -Preact-admin -DskipTests package` for packaging the React admin into the jar.
- State the legacy `/admin-legacy` status after the Scheme B deletion.
- Preserve the security boundary that MCP does not reuse management sessions and still requires Bearer Token.
- Delete the obsolete root `PLANS.md` and remove current docs references that point users to it.

## Non-Scope

- No runtime Java, React, Maven, or security behavior changes.
- Do not rewrite historical completed execution plans just because they record earlier Thymeleaf or `/admin-next`
  migration stages.
- Do not delete `docs/PLANS.md`; it remains the Harness execution index for this task.

## Assumptions

- The user's final "delete PLANS.md" request refers to the root-level `PLANS.md`, because `docs/PLANS.md` is listed as a
  document to update and is required by the current Harness manifest.
- Historical execution plan files are audit records and should not be rewritten unless they link users to the obsolete
  root plan.

## Acceptance Criteria

- [x] User-facing docs describe the management UI as React SPA rather than the active Thymeleaf backend.
- [x] `/login` and `/admin` are documented as the current entries.
- [x] `dbflow-admin/` and the React/Vite/TypeScript/shadcn/ui/Tailwind/TanStack stack are documented.
- [x] `./mvnw -Preact-admin -DskipTests package` is documented.
- [x] `/admin-legacy` is documented as deleted/unavailable.
- [x] MCP security documentation says management sessions are not reused and Bearer Token is still required.
- [x] Root `PLANS.md` is deleted.
- [x] Current user-visible docs do not claim the project avoids an independent frontend SPA.
- [x] `python3 scripts/check_harness.py` passes.

## Plan

- [x] Step 1: Preflight Harness and current documentation state.
- [x] Step 2: Update README, admin guide, deployment, architecture, and observability docs.
- [x] Step 3: Delete root `PLANS.md` and clean current references.
- [x] Step 4: Verify, record evidence, and archive this plan.

## Evidence Log

- 2026-05-03: Preflight `python3 scripts/check_harness.py` passed with 13 manifest artifacts.
- 2026-05-03: Current root `PLANS.md` exists and contains the obsolete React Admin 0-1 migration plan from the
  Thymeleaf era.
- 2026-05-03: Updated README, admin guide, deployment guide, architecture, and observability with React SPA cutover
  state, `dbflow-admin/`, frontend stack, packaging profile, `/admin-legacy` deletion, and MCP Bearer Token boundary.
- 2026-05-03: Deleted root `PLANS.md`.
- 2026-05-03: Current-doc scan found no root `PLANS.md` links and no "项目当前不引入独立前端 SPA" claim in the
  user-facing
  docs edited by this task.
- 2026-05-03: `python3 scripts/check_harness.py` passed with 13 manifest artifacts.
- 2026-05-03: `git diff --check` passed with no output.
