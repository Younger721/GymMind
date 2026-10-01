# Personal Profile And User Statistics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add role-scoped user statistics and a personal member page with a check-in QR code, date-range check-in counts, and package-based membership type display.

**Architecture:** Add two authenticated backend read APIs: one aggregates the current user's profile/check-in/membership data, and one provides role-scoped user statistics plus a read-only user list. Reuse the existing member check-in token, gym check-in table, membership package name, and menu permission model. Add a visible `/profile` route for all roles and remove user-row status actions from the admin view.

**Tech Stack:** Spring Boot 3.4, Spring Data JPA, Java 17, Vue 3, TypeScript, Element Plus, Vite.

## Global Constraints

- Backend operation permission codes do not authorize requests; menu visibility remains the frontend operation boundary.
- Platform administrators can read all users/statistics; gym administrators can read only their own gym's coaches, members, and their own account.
- Only members receive a check-in QR code; the QR payload is the existing member `checkInToken`.
- Check-in statistics use an inclusive start date and inclusive end date in the server business timezone.
- Membership type is derived from the membership package name containing `年卡`, `月卡`, or `周卡`; otherwise show the original package name.
- Do not expose passwords, refresh tokens, or unrelated tenant data.

---

### Task 1: Add failing backend tests for role-scoped statistics and personal overview

**Files:**
- Create: `backend/src/test/java/com/gymmind/iam/application/UserStatisticsServiceTest.java`
- Create: `backend/src/test/java/com/gymmind/profile/application/ProfileOverviewServiceTest.java`

**Interfaces:**
- `UserStatisticsService.list(actor, pageable)` returns a role-filtered `PageResponse<UserSummary>`.
- `UserStatisticsService.statistics(actor)` returns role totals and membership-type counts.
- `ProfileOverviewService.overview(actor, from, to)` returns account data, optional member QR data, check-in count, and optional active membership data.

- [ ] **Step 1: Write the failing role-scope tests**

  Assert a platform actor receives all users, while a gym admin receives only the current gym's coaches, members, and own account.

- [ ] **Step 2: Write the failing personal overview tests**

  Assert a member overview queries the member token, counts records inside the requested date range, and maps a package named `月卡-基础` to membership type `月卡`. Assert a non-member overview omits QR and membership data.

- [ ] **Step 3: Run the focused tests and verify the expected missing-service failures**

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'; $env:Path="$env:JAVA_HOME\bin;D:\apache-maven-3.9.9\bin;$env:Path"
mvn '-q' '-Dtest=UserStatisticsServiceTest,ProfileOverviewServiceTest' test
```

Expected: compilation/test failure because the new service contracts do not exist yet.

### Task 2: Implement backend profile and statistics services

**Files:**
- Create: `backend/src/main/java/com/gymmind/profile/application/ProfileOverviewService.java`
- Create: `backend/src/main/java/com/gymmind/profile/application/DefaultProfileOverviewService.java`
- Create: `backend/src/main/java/com/gymmind/profile/application/ProfileOverview.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/UserStatisticsService.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/DefaultUserStatisticsService.java`
- Create: `backend/src/main/java/com/gymmind/iam/application/UserStatisticsView.java`
- Modify: `backend/src/main/java/com/gymmind/booking/domain/repository/GymCheckInRepository.java`
- Modify: `backend/src/main/java/com/gymmind/booking/infrastructure/persistence/SpringDataGymCheckInRepository.java`
- Modify: `backend/src/main/java/com/gymmind/booking/infrastructure/persistence/JpaGymCheckInRepository.java`
- Modify: `backend/src/main/java/com/gymmind/membership/domain/repository/MemberMembershipRepository.java`
- Modify: `backend/src/main/java/com/gymmind/membership/infrastructure/persistence/SpringDataMemberMembershipRepository.java`
- Modify: `backend/src/main/java/com/gymmind/membership/infrastructure/persistence/JpaMemberMembershipRepository.java`
- Modify: `backend/src/main/java/com/gymmind/iam/domain/repository/UserRoleRepository.java`
- Modify: `backend/src/main/java/com/gymmind/iam/infrastructure/persistence/SpringDataUserRoleRepository.java`
- Modify: `backend/src/main/java/com/gymmind/iam/infrastructure/persistence/JpaUserRoleRepository.java`

**Interfaces:**
- Add a tenant/member/date-range check-in count method returning `long`.
- Add an active membership lookup that can also load the package name.
- Add a role-to-user ID lookup for `COACH` and `MEMBER` filtering.
- `ProfileOverview` includes account data, optional member token/status, requested dates, count, and optional package name/type/validity.
- `UserStatisticsView` includes total users, role totals, and membership-type counts.

- [ ] **Step 1: Add repository methods and JPA query declarations** using tenant-scoped count, active membership, and role lookup queries.
- [ ] **Step 2: Implement `DefaultProfileOverviewService`** with local-date range conversion, member-only QR data, and package-name type detection.
- [ ] **Step 3: Implement `DefaultUserStatisticsService`** with all-user platform scope and gym-admin scope limited to own account, coaches, and members.
- [ ] **Step 4: Run the focused tests and verify they pass.**

### Task 3: Expose authenticated profile and statistics APIs

**Files:**
- Create: `backend/src/main/java/com/gymmind/profile/api/ProfileController.java`
- Create: `backend/src/main/java/com/gymmind/iam/api/UserStatisticsController.java`
- Create: `backend/src/test/java/com/gymmind/profile/api/ProfileControllerTest.java`
- Create: `backend/src/test/java/com/gymmind/iam/api/UserStatisticsControllerTest.java`

**Interfaces:**
- `GET /api/v1/profile/overview?from=YYYY-MM-DD&to=YYYY-MM-DD` returns `ApiResponse<ProfileOverview>`.
- `GET /api/v1/users/statistics` returns `ApiResponse<UserStatisticsView>`.
- `GET /api/v1/users` returns the role-scoped read-only rows.

- [ ] **Step 1: Add MockMvc tests** for authenticated success and invalid date ranges.
- [ ] **Step 2: Implement controllers** with `CurrentActorProvider`, ISO dates, and current-month defaults for omitted profile dates.
- [ ] **Step 3: Run `mvn '-q' '-Dtest=ProfileControllerTest,UserStatisticsControllerTest' test` and verify PASS.**

### Task 4: Update frontend APIs, menu, profile page, and admin statistics view

**Files:**
- Modify: `frontend/src/api/profile.ts`
- Modify: `frontend/src/api/users.ts`
- Modify: `frontend/src/utils/menuPermissions.ts`
- Modify: `frontend/src/views/ProfileView.vue`
- Modify: `frontend/src/views/business/AdminView.vue`
- Modify: `frontend/package.json` and the frontend lockfile if a QR renderer is not already installed.

**Interfaces:**
- `profileApi.getOverview(from, to)` calls `/v1/profile/overview`.
- `usersApi.statistics()` calls `/v1/users/statistics`.
- `MENU_CODES.PROFILE` is visible to authenticated roles with the existing default menu assignment.

- [ ] **Step 1: Add typed API helpers** for profile overview, statistics, member QR, membership, and date-range data.
- [ ] **Step 2: Build `/profile`** with account details, date-range picker, check-in count, member QR rendering, package name/type/validity, and non-member empty states.
- [ ] **Step 3: Add `MENU_CODES.PROFILE` to the visible menu set.**
- [ ] **Step 4: Remove admin row status actions and create controls; add statistics cards and membership-type distribution above the read-only role-scoped list.**
- [ ] **Step 5: Run `npm run build` and verify `vue-tsc` and Vite exit 0.**

### Task 5: Restart and verify end to end

**Files:** No source files; verify running services and build artifacts.

- [ ] **Step 1: Build the backend with tests skipped only for packaging.**

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'; $env:Path="$env:JAVA_HOME\bin;D:\apache-maven-3.9.9\bin;$env:Path"
mvn '-Dmaven.test.skip=true' '-q' package
```

- [ ] **Step 2: Restart the existing GymMind Java/Vite processes** with root `.env`, JDK 17, backend port 8080, and frontend port 5173; verify `/actuator/health` returns 200.
- [ ] **Step 3: Verify platform admin sees all roles/users, gym admin sees own gym coaches/members/self, and members see QR/check-in/membership data.**
- [ ] **Step 4: Confirm `/admin` and `/profile` have no 403 operation-permission errors or frontend console errors; empty data renders as an empty state.**

