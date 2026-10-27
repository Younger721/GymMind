# 知识库上传：非 Markdown 文档规范化为 MD 再入库

> 状态：已实现  
> 关联：[2026-10-knowledge-chunk-artifact-minio.md](./2026-10-knowledge-chunk-artifact-minio.md)

## 目标

- 上传仍保留 **MinIO 原文件**（DOCX / HTML / PDF 等）。
- 入库阶段将 DOCX、HTML、PDF **转为 UTF-8 Markdown**，写入 `normalized/content.md`，再按 MD 结构化解析与切片。
- `.md` / `.txt` 不额外写 normalized 对象；解析路径与 ingest 一致。

## MinIO 路径

```text
{platform|tenant/{tenantId}}/knowledge/{uuid}/{fileName}              # 原文件
{platform|tenant/{tenantId}}/knowledge/{uuid}/normalized/content.md  # 规范化 MD
```

MySQL 字段：`knowledge_document.normalized_markdown_key`（可空）。

## 配置

```yaml
gymmind:
  knowledge:
    ingest:
      markdown-normalization:
        enabled: true
```

关闭时回退为直接 `parseStructured`（DOCX/PDF/HTML 旧分支）。

## 非目标

- 旧版 `.doc`、Office 其它格式、OCR、LLM 转写
- 历史文档批量回填（可另做脚本）
