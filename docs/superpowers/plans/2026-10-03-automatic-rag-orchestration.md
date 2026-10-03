# Automatic RAG Orchestration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the user-facing RAG toggle and make each AI consultation automatically retrieve permitted knowledge context while preserving LLM-controlled business tool calling.

**Architecture:** `DefaultAiHarnessService` will normalize every session request to include the configured and permitted `knowledge_search` capability, execute that retrieval before native tool orchestration, and pass the retrieved context to the model. Existing business capabilities remain LLM-selected through `AiToolOrchestrator`; the frontend keeps the API plugin field for compatibility but no longer exposes or derives behavior from a RAG switch.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, Mockito, Vue 3, TypeScript, Element Plus, Vite.

## Global Constraints

- Keep `knowledge_search` as a backend permission/configuration capability; remove only the session-level manual toggle.
- Preserve tenant and role permission boundaries for both knowledge retrieval and business tools.
- Do not save prompts, full answers, or API keys.
- Keep old requests containing `knowledge_search` compatible.
- Do not stage unrelated existing worktree changes.

### Task 1: Make knowledge retrieval automatic in the harness

**Files:**
- Modify: `backend/src/main/java/com/gymmind/ai/harness/application/DefaultAiHarnessService.java`
- Test: `backend/src/test/java/com/gymmind/ai/harness/DefaultAiHarnessProgressTest.java`

**Interfaces:**
- Consumes: configured tenant/platform plugin IDs, `AiHarnessPluginRegistry`, `AiCapabilityRegistry`, and existing `AiToolOrchestrator`.
- Produces: every eligible chat request executes `knowledge_search` once before native tool orchestration; native tools receive the resulting `ContextSegment` list.

- [ ] **Step 1: Write the failing test**

Add this test to `DefaultAiHarnessProgressTest`:

```java
@Test
void automaticallyRetrievesKnowledgeWhenSessionDoesNotSelectRag() {
    AiHarnessPlugin plugin = knowledgePlugin("自动检索依据");
    var tenants = mock(TenantHarnessConfigRepository.class);
    when(tenants.findByTenantId(7L)).thenReturn(Optional.of(
            TenantHarnessConfig.defaults(7L, "basic_chat,knowledge_search", "助手")));
    var model = mock(ChatModelGateway.class);
    when(model.stream(anyString(), anyList(), any())).thenReturn("answer");
    var events = new ArrayList<AiProgressEvent>();
    var service = new DefaultAiHarnessService(
            new AiHarnessPluginRegistry(List.of(plugin)), tenants,
            mock(PlatformHarnessConfigRepository.class), new AiHarnessProperties(), model, null, null);

    service.chat(new CurrentActor(1L, 7L, Set.of(RoleCode.GYM_ADMIN), Set.of("ai:chat"), 0, "t"),
            "session", "问题", List.of("basic_chat"), List.of(), ignored -> {}, events::add);

    assertThat(events).extracting(AiProgressEvent::pluginId).contains("knowledge_search");
    var context = ArgumentCaptor.forClass(List.class);
    verify(model).stream(anyString(), context.capture(), any());
    assertThat(context.getValue()).extracting("text").contains("自动检索依据");
}
```

Reuse the existing test helper pattern for `knowledgePlugin`, importing `any`, `anyList`, and `PlatformHarnessConfigRepository` as needed.

- [ ] **Step 2: Run the focused test to verify it fails**

Run from `backend`:

```powershell
./mvnw.cmd -Dtest=DefaultAiHarnessProgressTest test
```

Expected: the new test fails because the current session plugin intersection omits `knowledge_search` when the request only contains `basic_chat`. If test compilation is blocked by the existing unrelated `UserControllerTest` constructor mismatch, record that exact blocker and continue with the same red assertion validated by code inspection.

- [ ] **Step 3: Implement the minimal automatic flow**

Update plugin normalization so configured/permitted `knowledge_search` is added to the effective plugin set even when the session request only contains `basic_chat`. Execute that plugin before the native tool branch, skip it in the legacy plugin loop to avoid duplicate retrieval, and pass its context to `toolOrchestrator.decide`. Keep the existing early no-context response only for the non-native path; native tool calls must still be able to answer business questions.

- [ ] **Step 4: Run the focused test/build verification**

Run:

```powershell
./mvnw.cmd -Dmaven.test.skip=true package
```

Then rerun the focused test command. Expected: main compilation passes; the focused test passes when the unrelated baseline test compilation issue is absent, otherwise the output identifies only that pre-existing blocker.

- [ ] **Step 5: Commit**

```powershell
git add backend/src/main/java/com/gymmind/ai/harness/application/DefaultAiHarnessService.java backend/src/test/java/com/gymmind/ai/harness/DefaultAiHarnessProgressTest.java
git commit -m "feat: 自动执行知识库检索"
```

### Task 2: Remove the manual RAG control from the AI center

**Files:**
- Modify: `frontend/src/views/AssistantView.vue`

**Interfaces:**
- Consumes: existing `harnessApi.stream` request shape and progress/citation responses.
- Produces: AI center without a RAG toggle; chat requests remain backward-compatible and display automatic source/process information.

- [ ] **Step 1: Write the failing UI contract check**

Run this source-level contract command before the edit:

```powershell
$source = Get-Content -Raw 'src/views/AssistantView.vue'
if ($source -notmatch 'ragEnabled|toggleRag|RAG 未启用') { throw 'Expected the current manual RAG control to exist before the change' }
```

- [ ] **Step 2: Run the check to verify the current UI exposes the old behavior**

Run:

```powershell
$source = Get-Content -Raw 'src/views/AssistantView.vue'
if ($source -notmatch 'ragEnabled|toggleRag|RAG 未启用') { throw 'Baseline control not found' }
```

Expected before the implementation: the command exits successfully because the source still contains the RAG button/state.

- [ ] **Step 3: Implement the minimal UI change**

Remove the `Collection` icon import, RAG button markup, `ragAvailable`, `ragEnabled`, and `toggleRag`. Keep `sessionPlugins` initialized with `basic_chat` for request compatibility. Remove answer-mode derivation from the toggle and replace the header subtext with automatic permission-scoped knowledge/data wording. Keep process events and citations visible.

- [ ] **Step 4: Run the frontend verification**

Run:

```powershell
npm run build
```

Expected: `vue-tsc` and Vite complete successfully, and the source contains no manual RAG toggle references.

Verify the source contract after the build:

```powershell
$source = Get-Content -Raw 'src/views/AssistantView.vue'
if ($source -match 'ragEnabled|toggleRag|RAG 未启用') { throw 'Manual RAG control still exists' }
```

- [ ] **Step 5: Commit**

```powershell
git add frontend/src/views/AssistantView.vue
git commit -m "feat: 移除 AI 中心手动 RAG 开关"
```

### Task 3: Verify end-to-end behavior and repository scope

**Files:**
- Inspect only: `backend/src/main/java/com/gymmind/ai/harness/application/DefaultAiHarnessService.java`, `frontend/src/views/AssistantView.vue`, related tests.

- [ ] **Step 1: Run backend main compilation and frontend build**

```powershell
./mvnw.cmd -Dmaven.test.skip=true package
cd ..\frontend
npm run build
```

- [ ] **Step 2: Run the focused backend test command**

```powershell
cd ..\backend
./mvnw.cmd -Dtest=DefaultAiHarnessProgressTest test
```

Record the pre-existing `UserControllerTest` testCompile failure if it prevents execution.

- [ ] **Step 3: Check the final diff and status**

```powershell
git diff --check
git status --short
```

Confirm only the automatic RAG implementation files are staged in these commits; preserve unrelated worktree changes.
