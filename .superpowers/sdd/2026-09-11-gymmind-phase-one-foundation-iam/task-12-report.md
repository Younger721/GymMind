# Task 12 implementation report

Status: DONE

## Commit

- Commit: `0398e8c feat: 接入JWT认证过滤器与RBAC`
- Base: `14ebfcb`

## RED evidence

- `backend\\mvnw.cmd -f backend/pom.xml '-Dtest=JwtAuthenticationFilterTest,SecurityConfigurationTest,AuthControllerTest' test`
  initially failed during test compilation because the Task 12 production
  contracts (`PrincipalLoader`, `JwtAuthenticationFilter`, security handlers,
  `SecurityConfig`, and `AuthController`) did not exist.
- After the first implementation, the same target suite exposed and isolated
  three contract issues: surrounding whitespace failed `@Email`, the security
  probe controller was not registered in the MVC slice, and method-level
  `AuthorizationDeniedException` was handled as an internal error. Each was
  corrected with a focused test or boundary fix.
- A new login-whitespace case was then run RED before adding the `LoginRequest`
  canonical trim, proving the API preserves the agreed email normalization rule.

## Implementation

- Added stateless Spring Security configuration. Only POST register/login/
  refresh, health, and OpenAPI GET endpoints are public; logout and all other
  requests require authentication. CSRF, form login, HTTP Basic, request cache,
  and server sessions are disabled.
- Added a bearer JWT filter that verifies access-token type, reconstructs the
  principal from Redis and MySQL state, loads authoritative UserRole and
  RolePermission authorities, and clears the security context after each
  request. Any malformed token, dependency failure, revoked token, stale
  tokenVersion, disabled user/tenant, or invalid role projection returns the
  unified `UNAUTHENTICATED` response.
- Added REST authentication and access-denied handlers. Both filter-level and
  method-level authorization failures use the stable API envelope with
  `UNAUTHENTICATED` or `FORBIDDEN`.
- Added `/api/v1/auth/register|login|refresh|logout` controllers and validated
  request records. Tenant id and role selection are never accepted from the
  client. Logout takes the verified access token from the Authorization header
  and the refresh token from the request body.
- Added a response mapper exposing user identity, authoritative roles and
  permissions, and both token expirations without exposing password material.

## Tests

- `backend\\mvnw.cmd -f backend/pom.xml '-Dtest=JwtAuthenticationFilterTest,SecurityConfigurationTest,AuthControllerTest' test`
  passed: 14 tests, 0 failures, 0 errors.
- `backend\\mvnw.cmd -f backend/pom.xml test` passed: 84 tests, 0 failures,
  0 errors.
- `git diff --check` passed before staging; the staged diff is checked again
  before commit.

## Hygiene and self-review

- User-owned README, frontend, and unrelated documentation changes remain
  unstaged.
- No real credentials, tokens, API keys, or database passwords were added.
- `PrincipalLoader` never trusts roles or permissions from JWT claims for
  authorities; claims supply the signed lookup identity while the database is
  authoritative.

## Review

- Scoped review: clean; no Critical/Important findings.
- Deferred minor: `SecurityConfig` permits conventional Swagger UI wildcard
  paths in addition to the explicitly required health/OpenAPI paths.
