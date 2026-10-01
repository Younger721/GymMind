# AI and RAG Bug Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent stale or unauthorized knowledge from reaching the model, make RAG failures explicit, and keep the legacy, Agent, and Harness AI paths consistent and testable.

**Architecture:** Keep the existing hybrid keyword/vector retrieval design. Add database-backed visibility/status enforcement at the search boundary, make index lifecycle operations follow document state transitions, and carry the real document ID through `IndexedChunk` into `ContextSegment`. Preserve the current fast system prompt and SSE token filtering while making dependency failures observable and non-silent.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Maven, existing keyword/vector index ports, SSE `SseEmitter`.

## Global Constraints

- Preserve all unrelated user or historical working-tree changes.
- Do not expose reasoning tokens, API keys, full sensitive documents, or model prompts in logs.
- Keep tenant and private-user isolation enforced for every retrieval path.
- Keep the existing chunk size, overlap, RRF, rerank, context budget, and fast-answer limits unless a test proves they must change.
- Every behavior change must have a focused backend test before implementation.

---

### Task 1: Carry the real document ID through retrieval

**Files:**
- Modify: `backend/src/main/java/com/gymmind/knowledge/infrastructure/search/IndexedChunk.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/infrastructure/search/KnowledgeIndexingService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/domain/HybridRetriever.java`
- Test: `backend/src/test/java/com/gymmind/ai/domain/HybridRetrieverTest.java`

**Interfaces:**
- `IndexedChunk` exposes `documentId()` in addition to `id()`, tenant, owner, text, and vector.
- `HybridRetriever` creates `ContextSegment(chunk.id(), String.valueOf(chunk.documentId()), chunk.text())`.

- [ ] **Step 1: Write failing tests** for a chunk whose chunk ID differs from its document ID; assert the resulting context carries the document ID, and assert `DefaultKnowledgeSearchService` can apply the per-document limit to multiple chunks from one document.
- [ ] **Step 2: Run the focused tests** with `mvn -pl backend -Dtest=HybridRetrieverTest,KnowledgeSearchServiceTest test`; verify the current implementation fails because the second context field is the chunk ID.
- [ ] **Step 3: Add `documentId` to `IndexedChunk`** and update all constructors, index implementations, and test fixtures to preserve it.
- [ ] **Step 4: Update `HybridRetriever`** to map the document ID instead of the chunk ID, keeping chunk ID as the context segment ID.
- [ ] **Step 5: Re-run the focused tests** and verify source citations and the two-chunk-per-document limit use the actual document.

### Task 2: Enforce published and ready document visibility at search time

**Files:**
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/DefaultKnowledgeSearchService.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/KnowledgeDocumentRepository.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/infrastructure/persistence/JpaKnowledgeDocumentRepository.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/infrastructure/persistence/SpringDataKnowledgeDocumentRepository.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeVisibilityRevocationTest.java`

**Interfaces:**
- Add a repository lookup that loads searchable documents by tenant and candidate document IDs.
- Search accepts only documents whose status is `READY`, `published` is true, and whose visibility permits the current user.

- [ ] **Step 1: Add failing tests** for unpublished, parsing/failed, deleted, tenant-visible, and private-user documents; assert only published READY documents owned by the tenant and visible to the actor remain.
- [ ] **Step 2: Run `KnowledgeVisibilityRevocationTest`** and verify stale indexed chunks are currently returned.
- [ ] **Step 3: Add the repository query and a filtering helper** that preserves index ranking while removing invalid candidates.
- [ ] **Step 4: Apply the filter after hybrid retrieval and before rerank/context budgeting**; return an empty result when all candidates are invalid.
- [ ] **Step 5: Re-run the visibility tests** and add a regression assertion that `documentId` in every returned `ContextSegment` matches the database document.

### Task 3: Make document state changes update indexes

**Files:**
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/DefaultKnowledgeDocumentUseCase.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/KnowledgeIngestWorker.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/infrastructure/search/KnowledgeIndexingService.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeIngestDeleteRaceTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeDocumentUseCaseTest.java`

- [ ] **Step 1: Write failing tests** asserting unpublish deletes all document chunks, republish enqueues/reindexes the document, and delete remains safe when the chunk repository is empty.
- [ ] **Step 2: Run the focused tests** and verify unpublish currently leaves index entries.
- [ ] **Step 3: Update `publish(actor,id,false)`** to call `indexing.deleteDocument(document.tenantId(), document.id())` after the state change is validated.
- [ ] **Step 4: Make indexing deletion idempotent** and ensure failed ingest does not mark a document READY; preserve existing race handling.
- [ ] **Step 5: Re-run the tests** including the existing delete race tests.

### Task 4: Define explicit RAG dependency failure behavior

**Files:**
- Modify: `backend/src/main/java/com/gymmind/ai/domain/HybridRetriever.java`
- Modify: `backend/src/main/java/com/gymmind/ai/harness/plugins/KnowledgeSearchHarnessPlugin.java`
- Modify: `backend/src/main/java/com/gymmind/ai/harness/application/DefaultAiHarnessService.java`
- Modify: `backend/src/main/java/com/gymmind/shared/error/ErrorCode.java` only if an existing dependency error cannot represent the failure
- Test: `backend/src/test/java/com/gymmind/ai/harness/DefaultAiHarnessServiceTest.java`

- [ ] **Step 1: Write failing tests** for keyword/vector/embedding failure when `knowledge_search` is enabled; assert the request returns an explicit dependency error and never calls the model, including when `basic_chat` is also enabled.
- [ ] **Step 2: Run the harness tests** and verify failures are currently swallowed and the model can be called without RAG context.
- [ ] **Step 3: Return a typed retrieval result** distinguishing `NO_MATCH` from `DEPENDENCY_FAILURE`; retain partial results only when at least one retrieval backend succeeds.
- [ ] **Step 4: Stop swallowing knowledge-search dependency failures** in the plugin and Harness service, record a failed usage status, and return the existing API error contract.
- [ ] **Step 5: Re-run harness and controller tests** and verify normal no-match behavior still returns the safe no-context response.

### Task 5: Add safe observability for retrieval and model failures

**Files:**
- Modify: `backend/src/main/java/com/gymmind/ai/domain/HybridRetriever.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/DefaultKnowledgeSearchService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/harness/application/DefaultAiHarnessService.java`
- Test: `backend/src/test/java/com/gymmind/ai/domain/HybridRetrieverTest.java`

- [ ] **Step 1: Add tests** that verify failure classification/log calls without asserting sensitive payloads.
- [ ] **Step 2: Add structured warning/error logs** containing tenant ID, backend type, operation, and elapsed time; never log query text, context text, token values, or credentials.
- [ ] **Step 3: Ensure rerank failure remains a fallback to fused results**, while keyword/vector/embedding failure follows Task 4’s explicit contract.
- [ ] **Step 4: Run the focused tests** and inspect logs for the required fields and absence of sensitive content.

### Task 6: Unify legacy and Harness AI behavior

**Files:**
- Modify: `backend/src/main/java/com/gymmind/ai/application/DefaultAiChatService.java`
- Modify: `backend/src/main/java/com/gymmind/agent/application/DefaultAgentChatService.java`
- Modify: `backend/src/main/java/com/gymmind/ai/application/ChatModelGateway.java` if a shared prompt/stream method is required
- Test: `backend/src/test/java/com/gymmind/ai/application/AiChatServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/agent/application/AgentChatServiceTest.java`

- [ ] **Step 1: Add failing tests** asserting active Agent and legacy Chat use the same fast-answer rules, safe no-context behavior, and the same RAG visibility contract.
- [ ] **Step 2: Run the tests** to document the current divergence between `complete` and Harness `stream` paths.
- [ ] **Step 3: Extract one shared prompt builder or gateway policy** containing the existing quick-answer rules; retain tenant custom prompts before the enforced rules.
- [ ] **Step 4: Apply the policy to legacy and Agent calls** and preserve existing Agent-specific tools and permissions.
- [ ] **Step 5: Re-run AI and Agent tests** and verify no path emits reasoning content or bypasses RAG filtering.

### Task 7: Resolve platform knowledge scope explicitly

**Files:**
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/DefaultKnowledgeDocumentUseCase.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/DefaultKnowledgeSearchService.java`
- Modify: `backend/src/main/java/com/gymmind/knowledge/infrastructure/search/KnowledgeIndexingService.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeDocumentUseCaseTest.java`

- [ ] **Step 1: Add tests** covering platform upload, ingest, and search for platform-admin and tenant actors.
- [ ] **Step 2: Choose the existing product rule**: either reject platform documents with a clear validation error, or add an explicit platform-document scope that is intentionally visible to permitted tenants.
- [ ] **Step 3: Implement the chosen scope consistently** in upload, ingest, index, repository lookup, and search authorization; do not silently treat `tenantId=null` as tenant data.
- [ ] **Step 4: Re-run the scope tests** and document the permission rule in the knowledge API comments.

### Task 8: End-to-end verification

**Files:**
- Test: existing backend AI, knowledge, harness, and controller test suites
- Verify: `frontend/package.json`, backend Maven build configuration

- [ ] **Step 1: Run focused backend tests** for Tasks 1–7.
- [ ] **Step 2: Run the full backend test suite** with `mvn -pl backend test`.
- [ ] **Step 3: Build the frontend** with `npm run build` from `frontend`.
- [ ] **Step 4: Manually exercise `/chat` and `/chat/stream`** with a published document, then unpublish it and verify it is no longer cited; verify SSE receives `token`, `done`, and explicit `error` events.
- [ ] **Step 5: Record residual limitations** such as external search service availability or unavailable platform-scope requirements; do not claim completion without test output.
