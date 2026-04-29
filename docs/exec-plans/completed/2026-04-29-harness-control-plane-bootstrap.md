# Harness Control Plane Bootstrap Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `harness-execute` semantics to implement this plan task by task. Use `harness-dispatch` only for independent subtasks with disjoint write scopes. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Initialize the Refinex-DBFlow Harness Engineering control plane so future Spring Boot MCP implementation work has durable architecture, planning, observability, and verification anchors.

**Architecture:** The repository is currently an empty Maven/Spring Boot target with only `README.md`, `LICENSE`, and an approved architecture spec. Bootstrap must preserve unmanaged files, create the Harness control plane, and ground generated docs in the approved DBFlow MCP architecture rather than inventing source-code facts that do not exist yet.

**Tech Stack:** Planned stack is JDK 21, Maven, Spring Boot 3.5.13, Spring AI 1.1.4, Spring Cloud 2025.0.2, Spring Cloud Alibaba `2025.0.0.0`, MySQL 8 primary, MySQL 5.7 secondary.

**Design Source:** `docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md`

---

## Scope

Create or complete the Harness control plane:

- Root `AGENTS.md`
- `docs/ARCHITECTURE.md`
- `docs/OBSERVABILITY.md`
- `docs/PLANS.md`
- `docs/exec-plans/active/`
- `docs/exec-plans/completed/`
- `docs/exec-plans/specs/`
- `docs/exec-plans/tech-debt-tracker.md`
- `docs/generated/harness-manifest.md`
- `docs/references/`
- `scripts/check_harness.py`

Explicitly exclude application source scaffolding, Maven project generation, MCP tool implementation, Spring Security implementation, database migrations, and management UI. Those belong to later implementation plans derived from the approved architecture spec.

## Decisions

- Use `harness-bootstrap` for control-plane creation.
- Preserve `README.md` and `LICENSE` as unmanaged docs.
- Treat `docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md` as an approved design source.
- Do not create module-level `AGENTS.md` files because no source modules exist yet.
- Do not invent build/test commands. Until Maven files exist, observability must state that build/test/lint/run commands are not available yet and point to the future stack baseline.

## Files

- Create or modify: `AGENTS.md` - root navigation and project rules.
- Modify: `docs/PLANS.md` - active plan index.
- Create: `docs/ARCHITECTURE.md` - top-level structural map grounded in current repo and approved design.
- Create: `docs/OBSERVABILITY.md` - current verification state and future command expectations.
- Create: `docs/exec-plans/tech-debt-tracker.md` - known debt and missing scaffold notes.
- Create: `docs/generated/harness-manifest.md` - managed artifact inventory.
- Create: `scripts/check_harness.py` - local validation script copied from Harness Powers.
- Preserve: `README.md`, `LICENSE`, `docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md`.

## Verification Strategy

- Run `python3 scripts/check_harness.py`.
- Expected: validation passes with all manifest entries present and cross-links resolvable.
- Test baseline: no application tests exist yet; record `NO TESTS` in bootstrap report and technical debt tracker.

## Risks

- Cargo-cult docs: mitigated by grounding docs in current empty repo plus the approved architecture spec.
- Broken links: mitigated by running `scripts/check_harness.py`.
- Overwriting unmanaged docs: mitigated by preserving `README.md` and `LICENSE`.
- Planning drift: mitigated by linking this plan and the approved spec from `docs/PLANS.md` and `docs/ARCHITECTURE.md`.

## Task 1: Complete Bootstrap Reconnaissance

**Files:**

- Read: `README.md`
- Read: `LICENSE`
- Read: `docs/exec-plans/specs/2026-04-29-dbflow-mcp-architecture-design.md`

**Decision Trace:** Harness bootstrap Phase 1; approved architecture spec background and version decisions.

- [x] **Step 1: Confirm current repository shape**

Run:

```bash
find . -maxdepth 2 -type d \( -name .git -o -name target -o -name build -o -name dist \) -prune -o -type d -print
```

Expected: no application source directories or build files yet.

- [x] **Step 2: Confirm no test baseline exists**

Run:

```bash
rg --files -g 'pom.xml' -g 'build.gradle*' -g 'src/**' -g '*Test.java'
```

Expected: no build or test files are returned.

**Evidence:** Repository contains `README.md`, `LICENSE`, `docs/`, and no application source directories, build files, or Java test files.

## Task 2: Create Harness Control Plane Files

**Files:**

- Create: `AGENTS.md`
- Create: `docs/ARCHITECTURE.md`
- Create: `docs/OBSERVABILITY.md`
- Modify: `docs/PLANS.md`
- Create: `docs/exec-plans/tech-debt-tracker.md`
- Create: `docs/generated/harness-manifest.md`
- Create: `docs/exec-plans/active/.gitkeep`
- Create: `docs/exec-plans/completed/.gitkeep`
- Create: `docs/exec-plans/specs/.gitkeep`
- Create: `docs/references/.gitkeep`

**Decision Trace:** Bootstrap spec and DBFlow architecture decisions around Streamable HTTP MCP, Spring stack, SQL policy, audit, and empty-repo scope.

- [x] **Step 1: Write root navigation**

Create `AGENTS.md` as a concise table of contents. Include links to `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`, the approved spec, and this active plan.

- [x] **Step 2: Write architecture map**

Create `docs/ARCHITECTURE.md` that states the current repo has no application source yet, then records the approved DBFlow architecture as the implementation target.

- [x] **Step 3: Write observability map**

Create `docs/OBSERVABILITY.md` with current commands. Since no Maven project exists yet, mark build/test/lint/run as unavailable and require future implementation plans to replace them after scaffolding.

- [x] **Step 4: Write debt tracker**

Record missing application scaffold and missing test baseline as known bootstrap-time debt, not as bootstrap failures.

- [x] **Step 5: Generate manifest**

List all managed Harness artifacts and unmanaged docs.

**Evidence:** Created `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/exec-plans/tech-debt-tracker.md`, `docs/generated/harness-manifest.md`, managed execution-plan directories, and gitkeep files.

## Task 3: Install and Run Harness Validation

**Files:**

- Create: `scripts/check_harness.py`

**Decision Trace:** Harness bootstrap Phase 2.5 and Phase 3 verification.

- [x] **Step 1: Copy validation script**

Copy the Harness Powers validation script from `/Users/refinex/.codex/plugins/cache/refinex-personal/harness-powers/0.1.0/skills/harness-bootstrap/scripts/check_harness.py` to `scripts/check_harness.py` and make it executable.

- [x] **Step 2: Validate control plane**

Run:

```bash
python3 scripts/check_harness.py
```

Expected: exits 0 and reports a passing Harness validation.

**Evidence:** `python3 scripts/check_harness.py` found 13 manifest artifacts and passed with all checks clean.

## Task 4: Bootstrap Report

**Files:**

- Read: `git status --short`
- Read: validation output from Task 3.

**Decision Trace:** Harness bootstrap Phase 3.3.

- [x] **Step 1: Summarize created and preserved files**

Report all managed files created, unmanaged docs preserved, validation result, and test baseline.

- [x] **Step 2: Point to next lifecycle step**

State that future app implementation should use `harness-plan` or `harness-feat` from the approved architecture spec.

**Evidence:** Bootstrap report prepared in the final handoff; next lifecycle step is a feature implementation plan derived from the approved DBFlow MCP architecture spec.
