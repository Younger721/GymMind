# AI Chat Memory Persistence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve AI conversations across menu navigation and page reloads while keeping confirmed long-term user memory separate, scoped, editable, and privacy-safe.

**Architecture:** Store chat sessions and ordered messages in tenant/user-scoped database tables. The AI center keeps a stable session ID, loads its messages on mount, appends each user and assistant message through the backend, and exposes new-session/history controls. Existing `ai_memory` remains a separate confirmed-facts store; a later summarization step may write only user-confirmed summaries into it.

**Tech Stack:** Spring Boot, JPA, Java records/services/controllers, Vue 3, Pinia, Axios, existing SSE Harness API, JUnit 5, Maven, TypeScript/Vite.

## Global Constraints

- Preserve tenant isolation: every session and message query must constrain both `tenant_id` and `user_id`.
- Do not persist API keys, hidden reasoning, system prompts, or full plugin context as chat messages.
- Store only user-visible user text and assistant content; SSE tokens are assembled before persistence.
- Keep the existing `ai_memory` table separate from chat history.
- Preserve the current streaming UX and fast-answer prompt.
- Existing unrelated working-tree changes must not be reverted.

---

### Task 1: Add chat session and message domain persistence

**Files:**
- Create: `backend/src/main/java/com/gymmind/ai/chat/domain/model/AiChatSession.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/domain/model/AiChatMessage.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/domain/model/AiChatMessageRole.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/domain/repository/AiChatSessionRepository.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/domain/repository/AiChatMessageRepository.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/infrastructure/SpringDataAiChatSessionRepository.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/infrastructure/SpringDataAiChatMessageRepository.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/infrastructure/JpaAiChatSessionRepository.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/infrastructure/JpaAiChatMessageRepository.java`
- Test: `backend/src/test/java/com/gymmind/ai/chat/AiChatPersistenceTest.java`

**Interfaces:**
- Session fields: `id`, `sessionKey`, `tenantId`, `userId`, `title`, `createdAt`, `updatedAt`, `archived`.
- Message fields: `id`, `sessionId`, `role`, `content`, `sequence`, `createdAt`.
- Repository operations: find/create session by `(tenantId,userId,sessionKey)`, list recent sessions, load ordered messages, append message, archive/delete session.

- [ ] Write tests proving two users or two tenants cannot read each other’s sessions.
- [ ] Write tests proving message ordering is deterministic by `sequence` and duplicate sequence values are rejected or serialized safely.
- [ ] Run `mvn -q '-Dtest=AiChatPersistenceTest' test` and confirm the new tests fail before implementation.
- [ ] Implement entities, unique constraints, indexes, repository adapters, and a transaction-safe append operation.
- [ ] Re-run the focused persistence tests and verify tenant/user isolation and ordering.

### Task 2: Implement chat history application service and APIs

**Files:**
- Create: `backend/src/main/java/com/gymmind/ai/chat/application/AiChatHistoryService.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/application/DefaultAiChatHistoryService.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/application/AiChatSessionView.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/application/AiChatMessageView.java`
- Create: `backend/src/main/java/com/gymmind/ai/chat/api/AiChatHistoryController.java`
- Test: `backend/src/test/java/com/gymmind/ai/chat/DefaultAiChatHistoryServiceTest.java`

**Interfaces:**
- `GET /api/v1/ai/chat/sessions` returns the current user’s recent sessions.
- `GET /api/v1/ai/chat/sessions/{sessionKey}` returns ordered visible messages.
- `POST /api/v1/ai/chat/sessions` creates or returns a session for a client key.
- `DELETE /api/v1/ai/chat/sessions/{sessionKey}` archives the session and hides it from future lists.
- `POST /api/v1/ai/chat/sessions/{sessionKey}/messages` appends a user or assistant message after validating role and content length.

- [ ] Add failing service tests for missing tenant context, cross-user access, empty content, and archived session access.
- [ ] Run the focused service tests and verify current endpoints do not exist.
- [ ] Implement actor scope resolution with `ActorAccess.requireTenantId(actor)` and current `userId`.
- [ ] Implement session creation, recent-session listing, ordered message loading, archive, and append validation.
- [ ] Re-run service tests and add controller tests asserting JSON response shapes.

### Task 3: Persist Harness conversations without breaking SSE

**Files:**
- Modify: `backend/src/main/java/com/gymmind/ai/harness/application/DefaultAiHarnessService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/harness/api/AiHarnessController.java`
- Test: `backend/src/test/java/com/gymmind/ai/harness/DefaultAiHarnessServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/ai/harness/AiHarnessControllerTest.java`

**Interfaces:**
- Harness chat receives the existing `sessionId` and persists one user message before model invocation.
- The completed assistant answer is persisted exactly once after streaming finishes.
- On model failure, persist an assistant error marker only if the existing product policy wants failed attempts visible; otherwise leave the user message and return the existing error event.

- [ ] Add tests asserting JSON and SSE calls append one user and one assistant message to the selected session.
- [ ] Add a failure test proving a disconnected client or model exception does not duplicate the assistant message on retry.
- [ ] Inject `AiChatHistoryService` into the Harness service and save user content before calling the model.
- [ ] Accumulate the existing token stream in the service/controller path and save the final assistant content once before `done` is sent.
- [ ] Re-run Harness tests and verify token events, `done`, `error`, and persistence behavior.

### Task 4: Restore the conversation in the AI center

**Files:**
- Modify: `frontend/src/views/AssistantView.vue`
- Modify: `frontend/src/api/harness.ts`
- Create or modify: `frontend/src/api/aiChatHistory.ts`
- Test: `frontend/tests/assistant-history.spec.ts`

- [ ] Add a browser test that sends a message, navigates to another menu, returns to AI center, and sees the original messages.
- [ ] Add a test that refreshes the page and restores the same session without creating duplicate messages.
- [ ] Store a stable per-tenant/user `sessionKey` in local storage; clear it only when the user chooses “新建对话”, logs out, or changes tenant context.
- [ ] On mount, create/load the session and fetch ordered messages before enabling the input.
- [ ] Keep the current local optimistic message for streaming, but do not POST duplicate messages after the backend has persisted them.
- [ ] Add “新建对话” and recent-session controls; selecting a session loads its messages and updates the current key.
- [ ] Run `npm run build` and the focused browser test.

### Task 5: Make long-term memory usable and explicit

**Files:**
- Modify: `frontend/src/views/business/AiToolsView.vue` or create `frontend/src/views/business/AiMemoryView.vue`
- Create or modify: `frontend/src/api/aiMemory.ts`
- Modify: `backend/src/main/java/com/gymmind/ai/harness/config/AiHarnessProperties.java`
- Modify: tenant Harness default configuration/bootstrap if present
- Test: `backend/src/test/java/com/gymmind/ai/application/DefaultAiMemoryServiceTest.java`
- Test: `frontend/tests/ai-memory.spec.ts`

- [ ] Add UI/API tests for listing, adding, editing by same key, and deleting confirmed memories.
- [ ] Enable `ai_memory` in the tenant default plugin list only after verifying the tenant has `ai:memory` permission; platform-only basic chat remains unchanged.
- [ ] Expose memory entries with type, key, value, updated time, and delete action.
- [ ] Keep automatic extraction out of this task; add an explicit “记住这条” confirmation action as the first write path.
- [ ] Verify private memory never appears for another user in the same tenant.

### Task 6: Add confirmed summary workflow and retention rules

**Files:**
- Create: `backend/src/main/java/com/gymmind/ai/chat/application/AiConversationSummaryService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/application/DefaultAiMemoryService.java` only for validated summary writes
- Modify: `backend/src/main/java/com/gymmind/ai/chat/infrastructure/JpaAiChatMessageRepository.java`
- Test: `backend/src/test/java/com/gymmind/ai/chat/AiConversationSummaryServiceTest.java`

- [ ] Add a test proving only a user-confirmed summary can become `CONVERSATION_SUMMARY` memory.
- [ ] Add a test proving summaries are bounded, for example the last 20 messages or 4,000 characters before model summarization.
- [ ] Implement summary generation as an explicit service call, never inline inside every token callback.
- [ ] Store summary source session and updated timestamp; allow deletion through the existing memory API.
- [ ] Verify the model receives summary plus recent messages, not the entire unbounded transcript.

### Task 7: End-to-end verification and migration safety

**Files:**
- Modify: database migration location used by the project for `ai_chat_session` and `ai_chat_message`
- Test: backend AI chat, Harness, security, and migration tests
- Verify: `frontend/package.json`

- [ ] Run all focused backend tests from Tasks 1–6.
- [ ] Run the full backend test suite with `mvn -q test` from `backend`.
- [ ] Run `npm run build` from `frontend`.
- [ ] Manually verify: send message, switch menu, return, refresh, select old session, start new session, switch tenant, and confirm no cross-tenant history.
- [ ] Verify migration on an existing database leaves all existing users and `ai_memory` rows intact.
- [ ] Record any external database or model service limitations before declaring the feature complete.
