# Execution Plan: MCP Client Guide

Created: 2026-04-30
Status: Completed
Author: agent

## Objective

Provide a practical MCP client configuration and first-use guide for connecting Codex, Claude, OpenCode, and Copilot to
Refinex-DBFlow over Streamable HTTP with Bearer Token authentication.

## Scope

**In scope:**

- `docs/user-guide/mcp-clients.md`
- Documentation indexes that point users to the MCP client guide.
- Official-doc verification notes for version-sensitive client configuration formats.

**Out of scope:**

- Runtime MCP server changes.
- Real Token generation or storage automation.
- Client-specific plugin packaging beyond documented configuration snippets.

## Constraints

- Do not invent unsupported client configuration fields.
- Token examples must be fake and must not look like a usable DBFlow Token.
- If a client configuration path is version-sensitive, cite the official source or mark it as requiring version
  verification.
- Keep the guide usable for local LAN and reverse-proxied HTTPS deployments.

## Assumptions

- DBFlow MCP endpoint remains `/mcp` and uses Streamable HTTP.
- Every MCP request must include `Authorization: Bearer <DBFlow Token>`.
- Users obtain DBFlow Tokens from the management UI and grant project/environment access before running smoke prompts.

## Acceptance Criteria

- [x] AC-1: `docs/user-guide/mcp-clients.md` exists and covers URL, Bearer Token, LAN access, and common errors.
- [x] AC-2: The guide includes Codex, Claude, OpenCode, and Copilot configuration snippets.
- [x] AC-3: Version-sensitive client behavior is linked to official docs or marked for per-version verification.
- [x] AC-4: The guide includes first-use smoke prompts for list targets, inspect schema, explain SQL, execute SELECT,
  TRUNCATE confirmation, and DROP denial.
- [x] AC-5: `./mvnw test` and `python3 scripts/check_harness.py` pass.

## Implementation Steps

### Step 1: Write client guide

**Files:** `docs/user-guide/mcp-clients.md`
**Verification:** Inspect for all required client snippets, fake tokens, and smoke prompts.

Status: ✅ Done
Evidence: Added client guide with common DBFlow connection rules, Codex/Claude/OpenCode/Copilot snippets, fake Token
examples, read-only smoke prompt, high-risk policy smoke prompt, and common errors.
Deviations:

### Step 2: Update indexes

**Files:** `docs/ARCHITECTURE.md`, `docs/OBSERVABILITY.md`, `docs/PLANS.md`
**Verification:** Links point to the new guide and active plan is registered.

Status: ✅ Done
Evidence: Registered the active plan and linked the user guide from architecture and observability docs.
Deviations:

### Step 3: Verify repository baseline

**Files:** documentation only
**Verification:** Run `git diff --check`, `python3 scripts/check_harness.py`, and `./mvnw test`.

Status: ✅ Done
Evidence:

- `git diff --check` passed.
- `python3 scripts/check_harness.py` passed.
- `./mvnw test` passed with `Tests run: 135, Failures: 0, Errors: 0, Skipped: 10`.
  Deviations:

## Progress Log

| Step | Status | Evidence                                                | Notes |
|------|--------|---------------------------------------------------------|-------|
| 1    | ✅      | `docs/user-guide/mcp-clients.md` created.               |       |
| 2    | ✅      | Architecture, observability, and plans docs updated.    |       |
| 3    | ✅      | Diff check, Harness check, and full Maven tests passed. |       |

## Decision Log

| Decision                                 | Context                                                                                                                  | Alternatives Considered                           | Rationale                                                                                                                                                                                         |
|------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Claude section targets Claude Code first | "Claude" product surface differs across Claude Code, Claude Desktop, Claude.ai managed connectors, and API MCP connector | Present one universal Claude Desktop JSON snippet | Claude Code has explicit official HTTP/header configuration. Desktop/connectors are version and plan sensitive, so the guide marks them for per-version verification instead of inventing fields. |

## Completion Summary

Completed: 2026-04-30
Duration: 3 steps
All acceptance criteria: PASS

Summary:

- Added `docs/user-guide/mcp-clients.md` with common DBFlow MCP connection rules, fake-token examples, client
  configuration snippets, official-documentation links, and version-sensitive notes.
- Included Codex, Claude Code, OpenCode, and GitHub Copilot / VS Code setup examples for Streamable HTTP with Bearer
  Token authentication.
- Added first-use prompts for read-only list/inspect/explain/select smoke and high-risk TRUNCATE confirmation / DROP
  denial smoke.
- Updated architecture, observability, and plan indexes.
