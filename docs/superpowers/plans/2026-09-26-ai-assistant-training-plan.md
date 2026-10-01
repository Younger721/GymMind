# AI Assistant Training Plan Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Put knowledge Q&A and training-plan generation in the AI Assistant, using authorized recent workout data to create editable drafts that are only saved after confirmation.

**Architecture:** Keep Harness/RAG and structured plan generation as separate backend workflows behind one assistant UI mode selector. Add tenant/member-scoped workout history queries and role-aware target resolution; reuse the validated workout-plan save service with a narrowly scoped member-self path.

**Tech Stack:** Java 17, Spring Boot 3.4.12, Spring Data JPA, JUnit 5/Mockito, Vue 3, TypeScript, Element Plus, Vite.

## Global Constraints

- Preserve all pre-existing working-tree changes; relevant permission changes are already present in `DefaultAiWorkoutPlanDraftService`, `DefaultWorkoutPlanService`, `AiWorkoutPlanController`, and `router/index.ts`.
- Tenant and member authorization comes from `CurrentActor` and repository checks, never from request-supplied `tenantId` or a member's arbitrary `memberId`.
- Query only completed workout records for the target tenant/member from the prior eight weeks, capped at 30 records.
- Do not send raw workout notes, phone numbers, or other irrelevant personal data to the model.
- Model output is an editable draft; saving requires an explicit user action and server-side authorization is repeated at save time.
- Keep the legacy `/plan` URL as a redirect to `/assistant?mode=workout-plan`.
- Do not add dependencies or rewrite Harness, RAG, or workout-record creation.

---

### Task 1: Add bounded workout-history queries and summary

**Files:**
- Modify: `backend/src/main/java/com/gymmind/workout/domain/repository/WorkoutRecordRepository.java`
- Modify: `backend/src/main/java/com/gymmind/workout/domain/repository/WorkoutSetRepository.java`
- Modify: `backend/src/main/java/com/gymmind/workout/infrastructure/persistence/SpringDataWorkoutRecordRepository.java`
- Modify: `backend/src/main/java/com/gymmind/workout/infrastructure/persistence/SpringDataWorkoutSetRepository.java`
- Modify: `backend/src/main/java/com/gymmind/workout/infrastructure/persistence/JpaWorkoutRecordRepository.java`
- Modify: `backend/src/main/java/com/gymmind/workout/infrastructure/persistence/JpaWorkoutSetRepository.java`
- Create: `backend/src/main/java/com/gymmind/workout/application/WorkoutHistoryQueryService.java`
- Create: `backend/src/main/java/com/gymmind/workout/application/WorkoutHistorySummary.java`
- Test: `backend/src/test/java/com/gymmind/workout/application/WorkoutHistoryQueryServiceTest.java`

**Interfaces:**
- `WorkoutHistoryQueryService.summarize(Long tenantId, Long memberId, LocalDate since, LocalDate until, int limit)` returns a summary with record count and compact prompt text.
- Repository queries accept both tenant ID and member ID, filter `COMPLETED`, order newest first, and cap results before loading sets.
- Set retrieval is batched by record IDs; exercise names/IDs in the prompt come from the same tenant's exercise catalog.

- [x] Write tests for tenant/member/date/status scoping, limit 30, completed sets only, and omission of free-text notes.
- [x] Run `cd backend; .\mvnw.cmd -q -Dtest=WorkoutHistoryQueryServiceTest test` and confirm the new tests fail because the query API is missing.
- [x] Add repository methods and implement a compact summary over the returned records, sets, and tenant exercise catalog.
- [x] Re-run `cd backend; .\mvnw.cmd -q -Dtest=WorkoutHistoryQueryServiceTest test` and confirm all query-summary tests pass.

### Task 2: Resolve plan targets by actor and expose permitted coach choices

**Files:**
- Modify: `backend/src/main/java/com/gymmind/coach/domain/repository/CoachAssignmentRepository.java`
- Modify: `backend/src/main/java/com/gymmind/coach/infrastructure/persistence/SpringDataCoachAssignmentRepository.java`
- Modify: `backend/src/main/java/com/gymmind/coach/infrastructure/persistence/JpaCoachAssignmentRepository.java`
- Modify: `backend/src/main/java/com/gymmind/ai/application/AiWorkoutPlanDraftService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/application/DefaultAiWorkoutPlanDraftService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/api/AiWorkoutPlanController.java`
- Test: `backend/src/test/java/com/gymmind/ai/application/AiWorkoutPlanDraftServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/ai/api/AiWorkoutPlanControllerTest.java`

**Interfaces:**
- Member actors resolve their member record from `(tenantId, userId)`; request `memberId` is ignored or rejected if it differs.
- Coach actors may target only active assignments in their own tenant; gym admins/platform admins require tenant context and member-read/plan permission.
- Add `GET /api/v1/ai/workout-plan-targets` returning the current member for a member actor, assigned members for a coach, and tenant members for an authorized tenant administrator.
- Add `CoachAssignmentRepository.findActiveMemberIdsByTenantIdAndCoachId(Long tenantId, Long coachId)` for the coach target list.

- [ ] Test member self-resolution, mismatched member ID rejection, assigned coach access, unassigned coach denial, tenant admin scoping, and global platform admin denial without tenant context.
- [ ] Run `cd backend; .\mvnw.cmd -q -Dtest="AiWorkoutPlanDraftServiceTest,AiWorkoutPlanControllerTest" test` and confirm the authorization tests fail before implementation.
- [ ] Add the active-assignment query and implement role-aware target resolution using tenant-scoped repositories.
- [ ] Re-run those tests and confirm unauthorized IDs never reach history/model calls.

### Task 3: Inject history into structured plan generation

**Files:**
- Modify: `backend/src/main/java/com/gymmind/ai/application/AiWorkoutPlanDraftService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/application/DefaultAiWorkoutPlanDraftService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/api/AiWorkoutPlanController.java`
- Modify: `frontend/src/api/ai.ts` or create `frontend/src/api/workoutPlanDraft.ts`
- Test: `backend/src/test/java/com/gymmind/ai/application/AiWorkoutPlanDraftServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/ai/api/AiWorkoutPlanControllerTest.java`

**Interfaces:**
- Draft request carries `memberId?`, `request`, and `includeHistory` (default `true`). The actor-specific target resolver determines the actual member.
- Draft response is `{ plan, history: { included, recordCount, summary } }`; plan remains a `CreateWorkoutPlanCommand` and is not persisted.
- Model prompt includes the user's structured requirements, a tenant exercise catalog of IDs/names, and only the bounded history summary when opted in.

- [ ] Test that the model receives scoped history only when enabled, receives an explicit no-history marker for empty results, and is not called after authorization/quota failure.
- [ ] Run `cd backend; .\mvnw.cmd -q -Dtest=AiWorkoutPlanDraftServiceTest,AiWorkoutPlanControllerTest test` and verify the contract tests fail before implementation.
- [ ] Implement the draft response contract and inject the summary into the existing JSON generation/validation flow.
- [ ] Re-run the AI draft/controller tests and verify malformed model JSON still creates no plan.

### Task 4: Allow members to save and read only their own AI plan drafts

**Files:**
- Modify: `backend/src/main/java/com/gymmind/workout/application/DefaultWorkoutPlanService.java`
- Modify: `backend/src/main/java/com/gymmind/workout/domain/repository/WorkoutPlanRepository.java`
- Modify: `backend/src/main/java/com/gymmind/workout/infrastructure/persistence/SpringDataWorkoutPlanRepository.java`
- Modify: `backend/src/main/java/com/gymmind/workout/infrastructure/persistence/JpaWorkoutPlanRepository.java`
- Test: `backend/src/test/java/com/gymmind/workout/application/WorkoutPlanServiceTest.java`

**Interfaces:**
- Only `saveValidatedDraft` gains the member-self authorization path: role `MEMBER`, `ai:plan`, matching member record/user, same tenant, and valid tenant exercises.
- Generic plan creation and publishing remain admin/coach-only; members cannot target another member or publish plans.
- Member list/find queries return only plans whose `memberId` matches the actor's own member record.

- [ ] Test member self-save/list/find, cross-member denial, and that generic create/publish remain denied for member actors.
- [ ] Run `cd backend; .\mvnw.cmd -q -Dtest=WorkoutPlanServiceTest test` and confirm the member-path tests fail before implementation.
- [ ] Add the scoped member path without changing the existing admin/coach checks or permission changes.
- [ ] Re-run workout plan service/controller tests and verify admin/coach behavior remains unchanged.

### Task 5: Add assistant mode UI, editable draft review, and compatibility redirect

**Files:**
- Modify: `frontend/src/views/AssistantView.vue`
- Modify: `frontend/src/api/harness.ts`
- Create: `frontend/src/api/workoutPlanDraft.ts`
- Modify: `frontend/src/router/index.ts`
- Modify: `frontend/src/layouts/MainLayout.vue`

**Interfaces:**
- `GET /v1/ai/workout-plan-targets`, `POST /v1/ai/workout-plan-drafts`, and `POST /v1/workout-plans/drafts` are consumed through `apiClient`.
- The knowledge mode keeps current Harness plugin/config behavior; the plan mode renders goal/frequency/experience/duration/equipment/limitations fields, target selection where allowed, and an editable plan-items table.
- Save is disabled until a valid draft is present; save calls the validated draft endpoint and then offers a link to the saved plan.

- [ ] Add a mode-switch testable state in `AssistantView.vue`; mode changes clear the current conversation so contexts never mix.
- [ ] Add target loading, plan request, editable item fields, history disclosure/count, generation error, and explicit save flow.
- [ ] Change `/plan` to redirect to `/assistant?mode=workout-plan`; remove the duplicate plan navigation item while preserving role filters on the existing assistant route.
- [ ] Run `cd frontend; npm run build` and resolve Vue/TypeScript errors without touching unrelated frontend changes.

### Task 6: End-to-end regression verification

**Files:**
- No additional source files unless a regression is found.
- Test: all test files named in Tasks 1-5.

- [ ] Run focused backend tests: `cd backend; .\mvnw.cmd -q -Dtest="WorkoutHistoryQueryServiceTest,AiWorkoutPlanDraftServiceTest,AiWorkoutPlanControllerTest,WorkoutPlanServiceTest,WorkoutPlanControllerTest" test`.
- [ ] Run the backend full test suite with `cd backend; .\mvnw.cmd -q test` and record any unrelated pre-existing failures separately.
- [ ] Run `cd frontend; npm run build`.
- [ ] Review `git diff --check` and inspect the final diff to ensure only assistant-plan feature files changed beyond the plan file and all previously dirty files remain intact.
