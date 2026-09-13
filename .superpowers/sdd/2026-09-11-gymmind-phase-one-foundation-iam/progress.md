# SDD ledger — plan: docs/superpowers/plans/2026-09-11-gymmind-phase-one-foundation-iam.md

Execution started on main with explicit user direction to execute in the current project. User-owned unrelated worktree changes are excluded from staging.

Planning complete:
- Design commit: cd816d1
- Master roadmap commit: 9d9ed89
- Phase-one plan commit: 7f00274

Task 1: INTERRUPTED at user request on 2026-09-11; no Task 1 commit exists.
- Pending tracked changes: backend/pom.xml, GymMindApplication.java, and deletion of the legacy common/config/controller/dto/entity/repository/security/service Java trees.
- Pending untracked Task 1 files: backend/mvnw, backend/mvnw.cmd, backend/.mvn/wrapper/maven-wrapper.properties, and backend/src/test/java/com/gymmind/architecture/PackageLayoutTest.java.
- Temporary RED fixture currently exists at backend/src/main/java/com/gymmind/controller/LegacyProbe.java and must never be committed.
- The Maven RED command was terminated while running; no valid ArchUnit failure was captured after the resume attempt.
- Existing task-1-report.md was written prematurely and is not completion evidence; replace or append it after valid RED/GREEN verification and commit.
- No files are staged.

Resume Task 1 exactly here:
1. Set JAVA_HOME to D:\java\jdk-17.0.14.
2. Run `backend\mvnw.cmd -f backend/pom.xml -Dtest=PackageLayoutTest test` with LegacyProbe present and capture the expected forbidden-package FAIL.
3. Delete only LegacyProbe.java with apply_patch.
4. Re-run PackageLayoutTest, `backend\mvnw.cmd -f backend/pom.xml test`, and `backend\mvnw.cmd -f backend/pom.xml -Pintegration verify` to GREEN.
5. Run git diff --check and a staged-secret scan, explicitly stage only Task 1 backend files, then commit `refactor: 重建Spring Boot后端工程基线`.
6. Generate a review package from base 7f002748246ec1247e7718cded394ae387361d90 and complete the independent Task 1 review before starting Task 2.

Task 1: fix round 1/5 (1 addressed, 0 open - added `com.gymmind.config..` architecture guard; commits 3785fdb..9eebc89)
Task 1: complete (commits 7f00274..9eebc89, review clean)
Task 2: in progress (base 9eebc89; implementer phase1_task2_impl)
Task 2: minor (deferred): expected Spring context-startup failure test emits one WARN line; behavior and assertions are correct, final review to triage test-output noise.
Task 2: complete (commits 9eebc89..6953fcf, review clean)
Task 3: in progress (base 6953fcf; implementer phase1_task3_impl)
Task 3: minor (deferred): expected-error tests emit repeated MockMvc INFO/ERROR output; final review to triage test logging capture.
Task 3: minor (deferred): Callable trace propagation test does not mutation-test worker-thread MDC cleanup after success/failure.
Task 3: fix round 1/5 (2 addressed, 0 open - safe diagnostic logging and SDD report scope; commits f21524d..5455d4f)
Task 3: complete (commits 6953fcf..5455d4f, review clean)
Task 4: in progress (base 5455d4f; implementer phase1_task4_impl)
Task 4: minor (deferred): IT does not independently assert database UTC session/column precision beyond Instant mapping; final review to triage.
Task 4: complete (commits 5455d4f..82fe32c, review clean)
Task 5: in progress (base 82fe32c; implementer phase1_task5_impl)
Task 5: minor (deferred): expected duplicate/null constraint ITs emit Hibernate warning/error lines; final review to triage test logging noise.
Task 5: complete (commits 82fe32c..84c2b23, review clean)
Task 6: in progress (base 84c2b23; implementer phase1_task6_impl)
Task 6: fix round 1/5 (2 addressed, 0 open - transient compatibility getter remains non-authoritative; Task 14 must derive roles from UserRole; commits 6583a01..af32726)
Task 6: complete (commits 6583a01..af32726, review clean)
Task 7: minor (deferred): DatabaseInitializationLockIT cannot deterministically prove the second connection is already blocked inside GET_LOCK before releasing the first lock.
Task 7: minor (deferred): PlatformAdminInitializerIT does not separately cover email-only, password-only, and entirely absent platform-admin configuration.
Task 7: complete (commits af32726..1d5c337, review clean with 2 deferred minors)
Task 8: minor (deferred): CurrentActorTest does not cover non-positive tenantId, null role/permission sets, blank permission entries, or null tokenId as a compact invariant matrix.
Task 8: complete (commits 1d5c337..c84ab2c, review clean with 1 deferred minor)
Task 9: minor (deferred): JwtClaims immutable-set behavior is inherited from CurrentActor but lacks a direct regression assertion.
Task 9: minor (deferred): strict duplicate JSON claim rejection is configured but lacks an explicit duplicate-claim token test.
Task 9: complete (commits c84ab2c..2ecfd0c, scoped review clean after expiry-boundary and raw-claim-type fixes; 2 extra review attempts failed due model-service availability)
Task 10: minor (deferred): RedisSessionStoreIT obtains a direct RedisConnection for FLUSHDB without explicitly closing it; final review to triage test resource hygiene.
Task 10: complete (commits 2ecfd0c..92aa52a, spec and quality approved with 1 deferred minor)
Task 11: in progress (base 92aa52a; implementer task11_impl)
Task 11: minor (deferred): logout actor 匹配测试未逐字段变异 userId、tenantId、roles、permissions、tokenVersion 和 jti。
Task 11: minor (deferred): 不存在邮箱的登录路径未执行固定 dummy BCrypt，仍可能存在远程计时枚举差异。
Task 11: fix round 1/5 (2 addressed, 0 open - registerTenant 完整事务与 Redis Lua 原子 logout；commits f555a60..14ebfcb)
Task 11: complete (commits 92aa52a..14ebfcb, review clean with 2 deferred minors)
Task 12: minor (deferred): SecurityConfig permits conventional Swagger UI wildcard paths in addition to health/OpenAPI endpoints.
Task 12: complete (commits 14ebfcb..0398e8c, review clean with 1 deferred minor)
Task 13: fix round 1/1 (addressed permission bootstrap, tenant token invalidation on disable, duplicate admin email conflict; commit b078519)
Task 13: complete (commits 36dd43d..b078519, review findings addressed; MySQL IT remains environment-blocked by unavailable Docker)
Task 14: complete (commit 7e4b0f4, tenant user management and role assignment)
Task 15: complete (commit 1bb2986, tenant user invitation flow implemented, ordinary tests green; MySQL IT blocked by unavailable Docker)
Task 16: complete (commit c835cc6; ArchUnit tenant-isolation guards green; IDOR MySQL IT blocked by unavailable Docker)
Task 17: complete (commit c90b083; exact-target reset guard/scripts implemented; real reset blocked by missing MySQL credentials and unavailable Docker for IT)
Task 18: complete (latest Task 18 commit; OpenAPI contract and ordinary tests green; schema/smoke ITs blocked by unavailable Docker)

Environment note (2026-09-12): Docker Desktop 4.87 was recovered without deleting images or volumes. Stale Unix-socket runtime directories were moved to timestamped `.stale-*` backups, WSL was shut down, and IntelliJ/Cursor working sets were trimmed after HCS reported `0x800705aa` with under 0.5 GB free physical memory. Docker server 29.7.2 reached `running`, and the Redis 7 Testcontainers verification passed. If HCS resource exhaustion recurs, check free physical memory before retrying Docker.

Execution rulings from the Task 4-18 read-only plan audit (product semantics unchanged):
- Run the plan's root-qualified commands from the repository root; do not combine them with the contradictory "from backend/" wording.
- Keep the brief-required abstract `MySqlIntegrationTest` support type, but require empirical proof that the ordinary Surefire suite neither discovers it as a runnable test nor starts/connects to Docker; all concrete container tests remain `*IT`.
- Task 16 repository-name restrictions apply only to tenant-scoped business entities. Exclude the platform `TenantRepository`, global normalized-email authentication lookup, and system role/permission catalog lookups; their application-layer guards still enforce actor boundaries.
- Task 17 reset scripts default to dry-run and require explicit `-Execute` / `--execute` plus exact `DROP gymmind` confirmation. Capture and compare sorted non-`gymmind` schema sets before and after reset.
- Every integration test activates the `test` profile and obtains MySQL/Redis endpoints exclusively from Testcontainers. `DatabaseResetIT` targets only its container and can never reset local MySQL.
- Task 18 real dev startup must document/inject required JWT, MySQL, Redis, and one-time platform-admin environment values without printing or committing them.
- OpenAPI defines `bearerAuth` globally but attaches it only to protected operations; public register/login/refresh/invitation-accept operations remain unauthenticated.

Database status:
- The local MySQL 8 service was observed running on port 3306.
- The gymmind database has NOT been dropped, cleared, or recreated.
- Database reset remains Task 17 and still requires a fresh exact-target read-only check before execution.

Main roadmap Task 4: complete (audit persistence and tenant-scoped query; commit `feat: 建立租户级操作审计能力`).
Main roadmap Task 5: complete (member, coach, and assignment services; commits `52c28aa`, `1969877`, `2f3564b`, `683fccb`).
Main roadmap Task 6: complete (course scheduling and capacity rules; commit `760d9ef`).
Main roadmap Task 7: complete (membership packages, member entitlements, order creation, and offline payment API; commits `bc68d84`, `1f2d9ce`, `201dc4d`, `7ec86fe`, `c269121`, `88a9094`).
Main roadmap Task 8: partial (tenant-scoped booking and capacity state machine; check-in, entitlement consumption, and no-show scheduler remain; commit `d32ce7e`).
