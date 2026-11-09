# Runtime Validation Report

**Generated**: 2026-11-09T18:35:00-05:00
**Target**: `registro-login`

## Summary

| Step | Status | Exit Code | Details |
|---|---|---:|---|
| Startup | PASS | n/a | Existing Spring Boot debug session was already running on port 8080; no duplicate instance was launched. Public routes responded in Chromium. |
| Integration tests | PASS | 0 | `.\mvnw.cmd test`: 73 tests across 12 suites; 0 failures, 0 errors, 0 skipped. |
| Browser checks | PASS | n/a | Chromium checked `/`, `/login`, `/register` at 1280, 390, and 320 px; invalid login feedback and anonymous access redirects also passed. |
| Package | PASS | 0 | `.\mvnw.cmd -q -DskipTests package` produced `target/registro-login-0.0.1-SNAPSHOT.jar`. |

**Overall**: PASS, with the environment and scope limits below.

## Coverage

- Registration validation, local login, JWT registration/token/protected endpoint behavior, OIDC configuration, role-based access, repositories, user/freight services, demo seeders, reports, and network monitoring are covered by the Maven suites.
- Browser checks verified public page rendering, required login controls, failed-login alert behavior, no JavaScript page errors, no local HTTP 5xx responses, and no horizontal overflow on the checked public pages at the tested viewport sizes.
- Unauthenticated requests to `/dashboard` and `/admin/monitoring` redirected to login. Admin monitoring rendering and role access were verified by MockMvc integration tests.
- Monitoring unit tests exercised reachable/unreachable TCP targets, anomaly reporting, target validation, Windows event XML parsing, and rejection of XML document type declarations.

## Issues found and corrected

1. Repaired 131 reversibly corrupted text/icon sequences across eight Thymeleaf templates.
2. Updated obsolete UI test expectations to match the current home, login, and registration headings.
3. Fixed a login-page horizontal overflow at 320 px by overriding desktop-only padding at the mobile breakpoint.
4. Removed unsupported fixed infrastructure claims from the home, login, and admin overview pages.
5. Set `spring.jpa.open-in-view=false` in test configuration to match production; all tests still pass.

## Environment and remaining limits

- Node.js 26.7.0 and Playwright 1.65.0 were available; Chromium was installed and used.
- Docker CLI was unavailable. The Maven suite used the configured local MySQL-compatible database.
- Hibernate warned that the server reports database version `5.5.5`, below the MySQL dialect's supported minimum of 8.0. Confirm the actual database engine/version before deployment; MariaDB-compatible version reporting can produce this warning.
- The actual Windows Server/Event Log permissions and configured network targets cannot be verified on the deployment host here. The local tests cover the parser and TCP behavior, not production firewall policy, service health protocols, or Windows Server permissions.
- Runtime audit entries are currently retained in memory and will not survive application restart.

## Evidence

- Maven reports: `target/surefire-reports/TEST-*.xml`
- Browser verification: shared Chromium session at `http://localhost:8080`
