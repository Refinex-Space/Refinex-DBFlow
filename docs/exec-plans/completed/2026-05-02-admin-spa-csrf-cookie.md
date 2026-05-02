# Execution Plan: Admin SPA CSRF Cookie

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Expose the management CSRF token through the default `XSRF-TOKEN` cookie so the React SPA can submit mutation requests
with the `X-XSRF-TOKEN` header, while keeping CSRF enabled, preserving Thymeleaf form CSRF behavior, and leaving the
separate `/mcp` security chain unchanged.

## Scope

**In scope:**

- Update `AdminSecurityConfiguration` to use `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
- Add the Spring Security SPA CSRF request handler pattern so header tokens use the raw cookie token and Thymeleaf
  hidden `_csrf` parameters continue to work.
- Add `AdminCsrfSpaTests` for cookie/header behavior, missing-CSRF rejection, and form compatibility.
- Keep `/mcp` security configuration untouched.
- Update Harness plan/docs evidence.

**Out of scope:**

- Implementing React API client code.
- Changing `/mcp`, Actuator, or bearer-token security chains.
- Disabling CSRF for any management mutation route.
- Reworking existing Thymeleaf controllers or templates.

## Constraints

- CSRF must remain enabled on the management session chain.
- SPA mutation requests must use the `X-XSRF-TOKEN` header.
- Cookie name should remain Spring Security's default `XSRF-TOKEN`.
- Existing Thymeleaf forms that use hidden `_csrf` tokens must keep working.
- `/admin/api/**` remains protected by administrator authentication and CSRF.

## Assumptions

- `/login` is enough to bootstrap a CSRF cookie for browser clients because it is part of the management security chain.
- Tests can prove Thymeleaf compatibility through an existing CSRF-protected form POST with a request-parameter token.

## Acceptance Criteria

- [x] AC-1: `AdminSecurityConfiguration` uses `CookieCsrfTokenRepository.withHttpOnlyFalse()`.
- [x] AC-2: Browser-readable `XSRF-TOKEN` cookie is emitted on a safe management GET.
- [x] AC-3: SPA mutation CSRF header name is `X-XSRF-TOKEN`.
- [x] AC-4: CSRF remains enabled; POST `/admin/api/users` without CSRF is rejected.
- [x] AC-5: Existing Thymeleaf hidden `_csrf` parameter flow still works for a form POST.
- [x] AC-6: `/mcp` security chain is not modified.
- [x] AC-7: `./mvnw -Dtest=AdminCsrfSpaTests,AdminSecurityTests test` passes.

## Implementation Steps

### Step 1: Add red CSRF SPA tests

**Files:** `src/test/java/com/refinex/dbflow/security/AdminCsrfSpaTests.java`
**Verification:** targeted test fails before implementation because the `XSRF-TOKEN` cookie is not emitted.

Status: ✅ Completed
Evidence: `./mvnw -Dtest=AdminCsrfSpaTests,AdminSecurityTests test` failed before implementation because `/login`
did not emit the `XSRF-TOKEN` cookie and SPA header tests could not obtain a CSRF cookie.

### Step 2: Configure cookie CSRF repository and request handler

**Files:** `src/main/java/com/refinex/dbflow/security/configuration/AdminSecurityConfiguration.java`
**Verification:** targeted tests pass.

Status: ✅ Completed
Evidence: Added `CookieCsrfTokenRepository.withHttpOnlyFalse()` and `SpaCsrfTokenRequestHandler`; targeted test passed
with 9 tests, 0 failures, 0 errors, 0 skipped.

### Step 3: Verify, document, and archive

**Files:** docs and repository state
**Verification:** targeted Maven test, Harness validator, diff hygiene.

Status: ✅ Completed
Evidence: `./mvnw -Dtest=AdminCsrfSpaTests,AdminSecurityTests,McpSecurityTests test` passed with 15 tests,
0 failures, 0 errors, 0 skipped. `python3 scripts/check_harness.py` passed. `git diff --check` passed.

## Progress Log

| Step      | Status | Evidence                                                                                  | Notes                                                                          |
|-----------|--------|-------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------|
| Preflight | ✅      | Context7 Spring Security 6.5 CSRF docs; `python3 scripts/check_harness.py`; `./mvnw test` | Harness passed; baseline Maven test passed 196 tests, 0 failures, 10 skipped   |
| 1         | ✅      | Red targeted test failed before implementation                                            | No `XSRF-TOKEN` cookie was emitted from `/login`                               |
| 2         | ✅      | `./mvnw -Dtest=AdminCsrfSpaTests,AdminSecurityTests test`                                 | 9 tests, 0 failures, 0 errors, 0 skipped                                       |
| 3         | ✅      | Targeted Maven test plus MCP regression; Harness validator; `git diff --check`            | 15 tests, 0 failures, 0 errors, 0 skipped; Harness passed; diff hygiene passed |

## Decision Log

| Decision                                 | Context                                                               | Alternatives Considered          | Rationale                                                                 |
|------------------------------------------|-----------------------------------------------------------------------|----------------------------------|---------------------------------------------------------------------------|
| Use cookie CSRF repository               | React must read the token from browser JavaScript                     | Keep session/default repository  | `CookieCsrfTokenRepository.withHttpOnlyFalse()` is the Spring SPA pattern |
| Keep header and parameter token handlers | SPA uses raw cookie token; Thymeleaf hidden field uses rendered token | Only configure cookie repository | Preserves both SPA `X-XSRF-TOKEN` and SSR `_csrf` flows                   |
| Test a real form POST                    | Thymeleaf compatibility matters more than implementation detail       | Assert template internals only   | Real POST proves the server still accepts hidden parameter CSRF           |
