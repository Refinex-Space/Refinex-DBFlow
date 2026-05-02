# Execution Plan: React Admin Subpath Proxy

Created: 2026-05-02
Status: Completed
Author: agent

## Objective

Configure `dbflow-admin` so the React admin build can be served under `/admin-next/`, while local Vite development
proxies backend API, login, logout, and actuator requests to the Spring Boot app on port `8080`.

## Scope

**In scope:**

- Set Vite `base` to `/admin-next/`.
- Configure Vite dev server port `5173`.
- Add Vite dev proxy entries for `/admin/api`, `/login`, `/logout`, and `/actuator` to `http://localhost:8080`.
- Keep build output directory as `dist`.
- Update `dev` and `preview` package scripts to bind `0.0.0.0`.
- Normalize build-time social image meta references when they are needed for `/admin-next/` output correctness.
- Verify build output resource references start with `/admin-next/`.

**Out of scope:**

- Implementing or changing backend API routes.
- Changing Spring Security, login/logout behavior, actuator exposure, or admin MVC routes.
- Starting a dev server as a long-running process.
- Changing deployment packaging beyond Vite build configuration.

## Constraints

- Keep `build` script as `tsc -b && vite build`.
- Preserve `dist` as the build output directory.
- Do not modify `pom.xml`, `src/main/java/**`, or `src/main/resources/templates/**`.

## Assumptions

- The backend API prefix intended for the next React admin is `/admin/api`, while existing `/admin/**` Thymeleaf routes
  are not proxied by this prompt.
- Vite `base` is enough for build-time asset URL prefixing; server-side fallback routing for `/admin-next/**` will be
  handled in a later backend/deployment step if needed.

## Acceptance Criteria

- [x] AC-1: `dbflow-admin/vite.config.ts` sets `base: '/admin-next/'`.
- [x] AC-2: Vite dev server uses port `5173`.
- [x] AC-3: Dev proxy maps `/admin/api`, `/login`, `/logout`, and `/actuator` to `http://localhost:8080`.
- [x] AC-4: Vite build output directory remains `dist`.
- [x] AC-5: `dbflow-admin/package.json` scripts use `vite --host 0.0.0.0` for `dev` and
  `vite preview --host 0.0.0.0` for `preview`, while `build` remains `tsc -b && vite build`.
- [x] AC-6: `pnpm --dir dbflow-admin build` completes successfully.
- [x] AC-7: `dbflow-admin/dist/index.html` references built assets with `/admin-next/` prefixes.
- [x] AC-8: `pom.xml`, `src/main/java/**`, and `src/main/resources/templates/**` are not modified.

## Implementation Steps

### Step 1: Add Vite subpath and proxy configuration

**Files:** `dbflow-admin/vite.config.ts`
**Verification:** source inspection plus `pnpm --dir dbflow-admin build`

Status: ✅ Completed
Evidence: `rg -n "base: '/admin-next/'|port: 5173|/admin/api|/login|/logout|/actuator|outDir: 'dist'"
dbflow-admin/vite.config.ts`; `pnpm --dir dbflow-admin build`.

### Step 2: Update package scripts

**Files:** `dbflow-admin/package.json`
**Verification:** source inspection for script values

Status: ✅ Completed
Evidence: `node -e "const p=require('./dbflow-admin/package.json'); console.log(p.scripts.dev); console.log(p.scripts.build);
console.log(p.scripts.preview)"` reported the requested script values.

### Step 3: Normalize HTML resource references and verify build output

**Files:** `dbflow-admin/index.html`, repository state
**Verification:** frontend build, built `index.html` asset prefix inspection, Harness check, diff hygiene,
protected-path
diff checks

Status: ✅ Completed
Evidence: `pnpm --dir dbflow-admin build`; `rg -o '="/[^" ]+' dbflow-admin/dist/index.html`; `rg -n
'="/(?!admin-next/)' dbflow-admin/dist/index.html --pcre2` returned no matches; protected backend path diff checks
returned no matches.

## Progress Log

| Step      | Status | Evidence                                                                           | Notes                                                                                                         |
|-----------|--------|------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|
| Preflight | ✅      | `python3 scripts/check_harness.py`; `pnpm --dir dbflow-admin build`; `./mvnw test` | Harness and frontend build baseline passed; Maven baseline passed 191 tests, 0 failures, 0 errors, 10 skipped |
| 1         | ✅      | Vite config source inspection; `pnpm --dir dbflow-admin build`                     | Added `/admin-next/` base, port `5173`, proxy entries, and explicit `dist` outDir                             |
| 2         | ✅      | package script inspection                                                          | Updated `dev` and `preview`; preserved `build`                                                                |
| 3         | ✅      | `dist/index.html` resource prefix inspection; protected backend path diff checks   | Local absolute resource references in built HTML now start with `/admin-next/`                                |

## Decision Log

| Decision                          | Context                                              | Alternatives Considered               | Rationale                                                                   |
|-----------------------------------|------------------------------------------------------|---------------------------------------|-----------------------------------------------------------------------------|
| Proxy only requested backend URLs | User listed exact proxy prefixes                     | Proxy all `/admin` traffic            | Avoid hijacking existing Thymeleaf `/admin/**` routes during development    |
| Keep config static                | Target backend is explicitly `http://localhost:8080` | Add env-driven proxy target           | Smallest design that satisfies this prompt without speculative settings     |
| Use `%BASE_URL%` for meta images  | Vite did not rewrite one social image meta path      | Hardcode `/admin-next/` in both metas | Keeps source tied to Vite base while satisfying the built HTML prefix check |

## Completion Summary

Configured the React admin Vite build for `/admin-next/`, added requested development proxy entries to the Spring Boot
backend, kept `dist` as the build output, updated dev/preview scripts to bind `0.0.0.0`, and normalized social image
meta references so built local resource paths use `/admin-next/`.
