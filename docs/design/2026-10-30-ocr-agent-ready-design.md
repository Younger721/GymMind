# OCR 功能设计：知识库识图入库 + Agent 工具化（面向 AI 独立抽离）

> 状态：M1 已实现（网关 + DashScope 适配 + REST + 配额用量），M2 识图入库 / M3 Agent 工具化待开发
> 关联：[2026-10-13-knowledge-upload-md-normalization.md](./2026-10-13-knowledge-upload-md-normalization.md)（该文档曾将 OCR 列为非目标，本文正式立项）
> 日期：2026-10-30

## 1. 背景与目标

### 1.1 现状

- 知识库入库只支持 DOCX / HTML / PDF / MD / TXT，图片与扫描件无法进入 RAG 链路（`KnowledgeDocumentValidator` 仅校验 PDF 魔数）。
- AI 基础设施已收敛为三个端口：`ChatModelGateway` / `EmbeddingGateway` / `RerankGateway`，供应商适配统一在 `ai.infrastructure.bailian`（DashScope OpenAI 兼容模式），复用 `BailianCallRetrier`、`BailianUsageParser`、`BailianFailureMessages`。
- Agent 侧已有 `TenantAgent`（`enabledTools` 逗号分隔工具名）+ `DefaultMcpToolService`（`search_articles` / `import_article` / `knowledge_search`）+ `ai.harness.capability.AiCapability` 注册表。

### 1.2 目标

1. **图片/扫描件 → 文本 → 知识库**：上传图片经 OCR 得到 Markdown，直接复用现有"规范化 MD → 切片 → 双索引"入库管线。
2. **OCR 即 Agent 工具**：Agent 对话中可调用 `ocr_extract` 工具识别图片并引用结果，而不是只有一条孤立 REST 接口。
3. **为 AI 独立抽离做准备**：OCR 能力满足"抽离三约束"（见 §7），未来把 `ai` + `agent` + `mcp` + `ocr` 打包成独立 `gymmind-agent` 服务时，工具签名与调用契约不变。

### 1.3 非目标（本期不做）

- PDF 多页扫描件 OCR（需 PDFBox 渲染，二期）；`.doc` 等旧格式。
- 本地 Tesseract / PaddleOCR 部署（预留适配位，不实现）。
- 图片版面还原（表格线框重建）；识别结果为 Markdown 文本即可。

## 2. 总体架构

新建顶级模块 `com.gymmind.ocr`（与 `agent`、`mcp` 平级），坚持现有六边形分层：

```text
┌────────────────────────── frontend (Vue) ──────────────────────────┐
│  KnowledgeView（识图入库入口）   AgentManageView（工具开关）          │
└──────────────┬──────────────────────────────────┬──────────────────┘
               │ REST                              │ Agent 对话
┌──────────────▼──────────────────────────────────▼──────────────────┐
│ ocr/api        OcrController（REST：识别 / 上传引用）                │
├───────────────────────────────────────────────────────────────────┤
│ ocr/application                                                    │
│   OcrRecognitionService   鉴权·配额·用量·审计 → 调网关               │
│   OcrUploadService        原图落 MinIO，签发 ocrUploadId             │
│   OcrKnowledgeIngestor    OCR 结果 → Markdown → 复用知识库入库       │
├───────────────────────────────────────────────────────────────────┤
│ ocr/domain      OcrGateway（端口）  OcrRequest/OcrPage/OcrResult    │
├───────────────────────────────────────────────────────────────────┤
│ ocr/infrastructure                                                  │
│   DashScopeOcrAdapter（qwen-vl-ocr，复用 Bailian 重试/用量解析）      │
│   UnavailableOcrAdapter（无 Key 时降级 DEPENDENCY_UNAVAILABLE）       │
│   JpaOcrJobRepository / SpringDataOcrJobRepository                  │
├───────────────────────────────────────────────────────────────────┤
│ 依赖：knowledge（入库/对象存储）  mcp（工具注册）  ai（用量/网关风格）  │
└───────────────────────────────────────────────────────────────────┘
```

> 模块放置理由：`EmbeddingGateway` 等端口在 `ai.domain`，但 OCR 同时服务知识库与 Agent，且未来整包随 Agent 服务搬走，顶级 `ocr` 模块边界更清晰；`OcrGateway` 的代码风格（中文注释、重试、失败文案）完全对齐 `ai.infrastructure.bailian` 现有约定。

## 3. 领域模型（端口 + 记录）

```java
// 端口：与 ChatModelGateway/EmbeddingGateway/RerankGateway 同构，供应商可替换
public interface OcrGateway {
    /** 是否支持该内容类型（image/jpeg、image/png、image/webp、image/bmp） */
    boolean supports(String contentType);

    /** 识别图片，返回分页文本与规范化 Markdown */
    OcrResult recognize(OcrRequest request);
}

// 请求：二进制只在此层出现，工具/REST 编排层不直接搬运
public record OcrRequest(String fileName, String contentType, byte[] content, OcrOptions options) {}

// 选项：语言提示、是否输出 Markdown 表格等
public record OcrOptions(String languageHint, boolean markdownTables) {}

// 分页结果：confidence 可空（DashScope 不保证返回）
public record OcrPage(int pageNumber, String text, Double confidence) {}

// 结果：带用量与供应商请求 ID，便于对账与排障
public record OcrResult(List<OcrPage> pages, String markdown,
                        String provider, String model, String providerRequestId,
                        ModelUsage usage) {}
```

## 4. DashScope 适配要点

- 模型：`qwen3.5-ocr`（DashScope OpenAI 兼容模式，图片以 base64 data URL 放入 `messages[].content`），专为文档/表格/试卷/手写优化。
- 配置对齐现有 `gymmind.ai.dashscope.*` 风格，新增：

```yaml
gymmind:
  ai:
    dashscope:
      ocr-model: ${DASHSCOPE_OCR_MODEL:qwen3.5-ocr}
  ocr:
    enabled: ${OCR_ENABLED:true}
    provider: ${OCR_PROVIDER:dashscope}
    max-bytes: ${OCR_MAX_BYTES:10485760}        # 与知识库 10MB 上限对齐
    timeout-ms: ${OCR_TIMEOUT_MS:60000}
    daily-quota-per-tenant: ${OCR_DAILY_QUOTA:200}
```

### 4.1 免费模型调研结论（2026-10-30，来源：百炼官方文档）

| 候选模型 | 定位 | 免费额度状态 | 结论 |
|---|---|---|---|
| `qwen3.5-ocr` | **专用 OCR 模型**（文档/表格/试卷/手写优化） | 在新人免费额度发放范围内（不在 2026-10-29 下架名单），每模型通常 100 万 Token、开通起 90 天有效 | **首选**，未验证前先在控制台确认余量 |
| `qwen3.8-flash` | 通用视觉理解，低成本档，支持结构化输出 | 在发放范围内（现项目聊天模型同款，额度独立） | **备选**：`qwen3.5-ocr` 额度耗尽后自动回退目标的候选 |
| `qwen-vl-ocr` 系列（含 `-latest`） | 旧版 OCR 模型 | **2026-10-29 起免费额度对新用户下架**；老用户已发放的可用至到期/耗尽 | **不选**：新项目不建 |

规则要点（官方文档）：仅华北2（北京）地域模型有免费额度；每个模型（含日期快照）额度相互独立、不互通；同一 API Key 通用，调用时自动按"免费额度 > 资源包 > 节省计划 > 按量付费"顺序抵扣。

**落地注意事项**：

1. 本项目 `.env` 的 `DASHSCOPE_API_KEY` 已被聊天/向量/重排使用，账号已开通——OCR 用**同一个 Key**即可，剩余额度需在控制台「免费额度」页或模型广场 `qwen3.5-ocr` 详情页确认（页面显示如 `362,917/1,000,000`）；
2. 建议在控制台为 `qwen3.5-ocr` 开启**"免费额度用完即停"**：额度耗尽返回 HTTP 403（`AllocationQuota.FreeTierOnly`）而不再转按量扣费，适配器的 403 分支映射为 `DEPENDENCY_UNAVAILABLE` + 明确文案"OCR 免费额度已用尽"；
3. 双模型回退：`DashScopeOcrAdapter` 支持 `fallback-model` 配置（默认 `qwen3.8-flash`），专用模型 403/限流时自动降级一次，两个额度池独立，等于双份免费额度；
4. 额度余量降至 20%/耗尽时百炼会短信/站内信通知，无需额外开发监控。

- 复用 `BailianCallRetrier`（3 次退避）、`BailianUsageParser`（token 计量）、`BailianFailureMessages`（失败文案）；无 API Key 时装配 `UnavailableOcrAdapter`，调用抛 `DEPENDENCY_UNAVAILABLE`，与现有 RAG 降级路径一致。

## 5. REST API

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/ocr/uploads` | 上传原图 → MinIO `tenant/{tenantId}/ocr/{uuid}/source.png`，签发 `ocrUploadId`（有效期 24h） | `ocr:use` |
| POST | `/api/ocr/recognize/{uploadId}` | 同步识别已上传图片，返回 Markdown + 分页文本 | `ocr:use` |
| POST | `/api/ocr/documents/{uploadId}` | 识别后直接以 Markdown 入知识库（复用 `KnowledgeDocumentUseCase.upload`） | `ocr:use` + `knowledge:write` |

设计约束：

- **先上传后引用**：所有接口只传 `uploadId`，二进制不进 JSON、不进工具入参（这是抽离后契约可远程化的前提，见 §7）。
- **魔数校验**：PNG/JPEG/WebP/BMP 魔数 + 大小上限，仿照 `KnowledgeDocumentValidator` 对 PDF 魔数的写法；`contentType` 与魔数不一致直接 `VALIDATION_FAILED`。
- **租户隔离**：`ocrUploadId` 只能被同租户 Actor 读取；跨租户访问返回 `RESOURCE_NOT_FOUND`。

## 6. Agent 工具化（抽离准备的核心）

### 6.1 MCP 工具

`DefaultMcpToolService` 新增：

```java
private static final McpTool OCR = new McpTool(
        "ocr_extract", "识别已上传图片并返回 Markdown 文本", "ocr:use");
```

- 入参：`{"uploadId": "...", "languageHint": "zh"}`；出参：`{"markdown": "...", "pages": [...], "uploadId": "..."}`
- `TenantAgent.enabledTools` 增加可选值 `ocr_extract`，`AgentManageView` 工具开关列表同步展示。

### 6.2 AiCapability 注册

新增 `OcrExtractCapability implements AiCapability`，`CapabilityDescriptor`：`permission = "ocr:use"`、`requiresTenant = true`，与 `MemberTrainingContextCapability` 等现有能力并列注册到 `AiCapabilityRegistry`。Harness 会话与 MCP 工具共享同一实现，避免双份逻辑。

### 6.3 识别结果的两条去向

1. **对话引用**：OCR 结果作为 `ContextSegment` 进入当轮上下文（带来源标注 `ocr:{uploadId}`），Agent 据此回答；
2. **知识沉淀**：Agent 回答后可调用入库工具（或用户在界面一键"存入知识库"），Markdown 写入对象存储 `.../ocr/{uuid}/normalized/content.md`，走既有切片/索引管线。

## 7. AI 独立抽离的演进路径（三条硬性设计约束）

| 约束 | 现在的实现 | 抽离后 |
|---|---|---|
| ① 工具签名稳定 | `ocr_extract(uploadId, options)`，文件以 ID 引用 | 签名不变，`ocr` 模块内 `McpToolInvoker` 从本地 Bean 调用切换为 HTTP/MCP 远程调用，上层无感 |
| ② 上下文显式传递 | `CurrentActor`（tenantId、userId、权限）作为方法参数显式传入，不依赖线程上下文快照 | Actor 随调用序列化为受信凭证（内部服务 Token），跨服务不丢租户隔离 |
| ③ 结果契约可回灌 | 统一输出 Markdown + 分页 + 来源 ID，可直接转 `ContextSegment` 或知识文档 | 相同 JSON 契约经网络传输，入库路径不变 |

```text
现在：单体  [agent chat] →(进程内)→ ocr/application → OcrGateway → DashScope
中期：抽离  [GymMind 主服务] →(MCP over HTTP)→ [gymmind-agent 服务: ai+agent+mcp+ocr] → DashScope
```

迁移时唯一新增的是 agent 服务与主服务之间的**服务间鉴权**（内部凭证替代用户 JWT 直传），`ocr` 模块代码不动。

## 8. 数据模型与对象存储

```text
ocr_job（租户隔离表）
  id BIGINT PK
  tenant_id BIGINT NOT NULL
  upload_key      VARCHAR(512)   -- MinIO 原图键
  status          VARCHAR(16)    -- PENDING/RUNNING/SUCCEEDED/FAILED
  provider        VARCHAR(32)
  model           VARCHAR(64)
  page_count      INT
  markdown_key    VARCHAR(512)   -- 识别结果 MD 对象键（入库时复用）
  error           VARCHAR(512)
  input_tokens / output_tokens BIGINT   -- 计量对账
  created_by_user_id BIGINT
  created_at / finished_at DATETIME
```

MinIO 路径沿用 knowledge 风格：

```text
tenant/{tenantId}/ocr/{uuid}/source.png           # 原图
tenant/{tenantId}/ocr/{uuid}/normalized/content.md # 识别结果
```

同步接口也落 `ocr_job` 记录：一是用量审计，二是二期切异步任务（PENDING→RUNNING）时表结构不变。

## 9. 配额、用量与安全

- **配额**：`TenantQuotaService` 新增 `ensureOcrAllowed(Long tenantId)`，按 `ocr_job` 当日成功次数计数，超限抛业务异常（对齐现有 `ensureKnowledgeUploadAllowed` 模式）。
- **用量**：识别完成后经 `DefaultAiUsageService` 记录，`scene = "ocr"`，复用计费模型。
- **注入防护**：图片无 PromptInjection 风险，但识别出的 Markdown 入知识库前必须过 `DocumentMarkdownNormalizer` 清洗，与现有上传路径同权。
- **审计**：复用 `AuditRecorder` 记录 `OCR_RECOGNIZE` / `OCR_INGEST` 操作事件。

## 10. 前端改动

- `KnowledgeView`：上传区支持图片类型，选择"识图入库"，展示识别预览（Markdown）→ 确认入库。
- `AgentManageView`：工具开关增加 `ocr_extract`。
- `AssistantView`：对话附件按钮 → 先 `/api/ocr/uploads` 再随消息引用 `uploadId`。

## 11. 分期实施

| 阶段 | 内容 | 交付判据 |
|---|---|---|
| M1 | `OcrGateway` + DashScope 适配 + `/api/ocr/uploads`、`/api/ocr/recognize` + 配额/用量 + `ocr_job` | 上传图片返回 Markdown；无 Key 时降级不崩 |
| M2 | `/api/ocr/documents` 识图入库，打通 normalized MD → 切片 → 索引；`KnowledgeView` 识图入口 | 入库后 `knowledge_search` 能召回图片内容 |
| M3 | MCP 工具 `ocr_extract` + `OcrExtractCapability` + Agent 开关；对话内引用 OCR 结果 | Agent 对话中可用 OCR 工具并引用识别文本 |
| M4（抽离期） | PDF 多页、异步队列、Tesseract 本地备选、`gymmind-agent` 独立服务化 | 工具契约不变，进程内调用切换为远程调用 |

## 12. 测试要点

- 魔数/大小/类型不匹配 → `VALIDATION_FAILED`；跨租户 `uploadId` → `RESOURCE_NOT_FOUND`。
- 无 API Key → `DEPENDENCY_UNAVAILABLE`（模拟 `UnavailableOcrAdapter`）。
- 配额超限、用量计量字段非空、`ocr_job` 状态机流转。
- Mock `OcrGateway` 的工具调用单测：权限缺失 → `FORBIDDEN`；`enabledTools` 未含 `ocr_extract` → 工具不可见。
- 入库回归：OCR Markdown 与手工上传 MD 走同一管线，`knowledge_search` 召回一致。
