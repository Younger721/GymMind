# Knowledge QA Fixes Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make knowledge-only answers disclose missing evidence, prefer tenant price rules over platform defaults, and preserve external rerank order except for explicit FAQ mismatch protection.

**Architecture:** Keep the changes within `DefaultAiHarnessService` and `DefaultKnowledgeSearchService`. Add integration-style unit tests through the existing Harness and knowledge search fixtures; no API, schema, or index changes are needed.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, AssertJ, Mockito, Maven.

## Global Constraints

- Compile and test with JDK 17, as required by `backend/pom.xml`.
- Preserve existing unrelated working-tree changes; stage only task-specific changes.
- Commit task changes using the repository format `<type>: <中文说明>`.
- Follow TDD: add each behavior test, observe the expected failure, then implement.

---

### Task 1: Disclose Missing Knowledge Evidence

**Files:**
- Modify: `backend/src/main/java/com/gymmind/ai/harness/application/DefaultAiHarnessService.java`
- Test: `backend/src/test/java/com/gymmind/ai/harness/AutomaticKnowledgeRetrievalTest.java`

**Interfaces:**
- `DefaultAiHarnessService.chat(...)` continues to return `AiChatView` and use the existing `AiHarnessProperties.EmptyKnowledgePolicy` setting.
- The test uses a `knowledge_search` plugin that adds no context and a mocked model response containing the existing no-evidence phrase.

- [ ] **Step 1: Write the failing DISCLOSE test**

Add a test using `LEGACY_ALWAYS_RAG`, an empty knowledge plugin, the default `DISCLOSE` policy, and a model response of `知识库未提供依据。以下内容仅供一般参考。`. Capture the prompt and assert it asks the model to disclose the missing direct knowledge basis and label any advice as general reference; assert the final answer still contains `知识库未提供依据。`.

```java
when(model.stream(anyString(), anyList(), any()))
        .thenReturn("知识库未提供依据。以下内容仅供一般参考。");
AiChatView result = service.chat(actor, "session", "运动损伤处理规范是什么？",
        List.of("basic_chat"), List.of(), ignored -> { }, ignored -> { });
ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
verify(model).stream(prompt.capture(), anyList(), any());
assertThat(prompt.getValue()).contains("没有找到直接相关的知识库资料", "一般参考");
assertThat(result.answer()).contains("知识库未提供依据。");
```

- [ ] **Step 2: Run the test and confirm the current bug**

Run with JDK 17:

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f backend/pom.xml "-Dtest=AutomaticKnowledgeRetrievalTest" test
```

Expected: the new test fails because the current prompt forbids disclosure and answer normalization strips the no-evidence phrase.

- [ ] **Step 3: Implement policy-aware empty-result rules**

For `KNOWLEDGE_ONLY` with no retrieved knowledge, make `DISCLOSE` instruct the model to say no direct knowledge basis was found and label any general advice as unverified reference. Preserve that disclosure through answer normalization. Keep `REFUSE` as a pre-generation return based on no retrieved knowledge.

- [ ] **Step 4: Re-run the Harness regression test**

Run the command from Step 2. Expected: all `AutomaticKnowledgeRetrievalTest` cases pass.

- [ ] **Step 5: Commit Task 1**

Use commit message `fix: 无知识命中时披露依据不足` and stage only the task's changes.

### Task 2: Apply Tenant Price and Promotion Overrides

**Files:**
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/DefaultKnowledgeSearchService.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgePriceResolutionTest.java`

**Interfaces:**
- The existing `KnowledgeDocumentRepository.findPriceRules(Long tenantId)` provides tenant rules for the actor's tenant and platform rules when passed `null`.
- Search results remain `ContextSegment` objects with the current `PRICE_RESULT` authoritative-answer format.

- [ ] **Step 1: Write the failing scope-precedence tests**

Add fixtures for tenant and platform documents. Verify that a tenant base price and active tenant promotion produce the quote even when platform defaults differ, and that platform rules remain usable when the tenant has no matching rule for the queried product. Assert the selected source document IDs so shadowed platform rules are not cited as the applied rules.

```java
var storeBase = price(7L, 501L, "store-month", 1, "门店月卡", "90");
var storeSale = promotion(7L, 502L, "store-sale", "门店活动", "0.90", 0);
var platformBase = price(null, 503L, "platform-month", 1, "平台月卡默认价", "100");
var platformSale = promotion(null, 504L, "platform-sale", "平台活动", "0.50", 99);
var result = service(List.of(storeBase, storeSale), List.of(platformBase, platformSale))
        .search(ADMIN, "月卡多少钱？");
assertThat(result).filteredOn(ContextSegment::authoritativeAnswer).singleElement()
        .satisfies(answer -> assertThat(answer.text()).contains("81元"));
assertThat(result).extracting(ContextSegment::documentId)
        .contains("501", "502").doesNotContain("503", "504");
```

For platform fallback, give the tenant only a year-card price, give the platform a month-card price of `100` and an active `0.90` promotion, then query `月卡多少钱？`; assert the authoritative answer contains `90元` and cites the platform month-card rules.

- [ ] **Step 2: Run the price tests and confirm the current bug**

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f backend/pom.xml "-Dtest=KnowledgePriceResolutionTest" test
```

Expected: the tenant-override case fails with a multiple-price/manual-confirmation answer instead of the tenant quote.

- [ ] **Step 3: Select applicable rules by scope**

For each product, use tenant base prices when any current published tenant base price exists; otherwise use platform base prices. For promotions, use tenant promotions that are active or have incomplete effective dates when present; otherwise use platform promotions that are active or have incomplete effective dates. Ignore expired and not-yet-effective tenant promotions for scope selection. Preserve conflict handling within the selected scope.

- [ ] **Step 4: Re-run price tests**

Run the command from Step 2. Expected: tenant overrides, platform fallback, latest-version selection, and existing promotion-conflict tests pass.

- [ ] **Step 5: Commit Task 2**

Use commit message `fix: 门店价目覆盖平台默认规则` and stage only the task's changes.

### Task 3: Preserve External Rerank Order

**Files:**
- Modify: `backend/src/main/java/com/gymmind/knowledge/application/DefaultKnowledgeSearchService.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeSearchRelevanceTest.java`

**Interfaces:**
- `RerankGateway.rerank(String, List<ContextSegment>)` continues to return ordered `RankedChunk` values.
- `applyExternalRerankOrder(...)` continues to return every eligible fused candidate, including candidates omitted by external `top_n`.

- [ ] **Step 1: Write a failing ordering test**

Use query `深蹲训练会员`. Candidate `local-high` is `深蹲训练会员动作规范` and candidate `rerank-first` is `深蹲训练安全说明`. Return only `rerank-first` from the mock reranker. Both candidates pass the lexical evidence filter, while `local-high` has the larger local score. Assert `rerank-first` is first and `local-high` remains after it.

```java
when(rerank.rerank(eq("深蹲训练会员"), any()))
        .thenReturn(List.of(new RankedChunk("rerank-first", 1)));
var result = service.search(actor, "深蹲训练会员");
assertThat(result).extracting(ContextSegment::id)
        .containsSubsequence("rerank-first", "local-high");
```

- [ ] **Step 2: Run the relevance tests and confirm the current bug**

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f backend/pom.xml "-Dtest=KnowledgeSearchRelevanceTest" test
```

Expected: the new test fails because local lexical score sorts ahead of the external rank.

- [ ] **Step 3: Make external rank the normal primary sort key**

Sort first by the hard FAQ mismatch flag for how-to questions, then by external rank, then by local tie-break score. Assign candidates absent from the external response a rank after all returned candidates. Keep the existing action-question FAQ regression behavior.

- [ ] **Step 4: Re-run relevance tests**

Run the command from Step 2. Expected: the new ordering case and existing FAQ mismatch case pass.

- [ ] **Step 5: Commit Task 3**

Use commit message `fix: 保留知识检索外部重排顺序` and stage only the task's changes.

### Task 4: Run Combined Knowledge QA Regression Tests

**Files:**
- Test: `backend/src/test/java/com/gymmind/ai/harness/AutomaticKnowledgeRetrievalTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgePriceResolutionTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeSearchRelevanceTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeSearchServiceTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeSearchRerankRetryTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeSearchDegradationTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeSearchHarnessAlignmentTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeSearchProgressTest.java`
- Test: `backend/src/test/java/com/gymmind/knowledge/application/KnowledgeDocumentUseCaseTest.java`

- [ ] **Step 1: Run the combined targeted suite**

```powershell
$env:JAVA_HOME='D:\java\jdk-17.0.14'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f backend/pom.xml "-Dtest=AutomaticKnowledgeRetrievalTest,KnowledgePriceResolutionTest,KnowledgeSearchRelevanceTest,KnowledgeSearchServiceTest,KnowledgeSearchRerankRetryTest,KnowledgeSearchDegradationTest,KnowledgeSearchHarnessAlignmentTest,KnowledgeSearchProgressTest,KnowledgeDocumentUseCaseTest" test
```

Expected: all targeted Harness and knowledge search tests pass.

- [ ] **Step 2: Check the final diff and worktree**

Run `git diff --check`, inspect the staged patch, and confirm unrelated pre-existing changes remain unstaged.
