package com.gymmind.knowledge.infrastructure.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredChunkerTest {
    private final StructuredChunker chunker = new StructuredChunker(new HeuristicTokenCounter());

    @Test
    void keepsProcedureTitleAndStepsTogether() {
        ParsedDocument document = new ParsedDocument(List.of(
                DocumentBlock.title("深蹲"),
                DocumentBlock.listItem("站稳并收紧核心", 0),
                DocumentBlock.listItem("屈膝下蹲", 0)));

        List<Chunk> chunks = chunker.chunk(document, "doc-1", ChunkStrategy.PROCEDURE);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).contains("深蹲", "站稳", "屈膝");
    }

    @Test
    void carriesTableHeadersIntoEveryRecordChunk() {
        ParsedDocument document = new ParsedDocument(List.of(
                DocumentBlock.tableRow(List.of("套餐", "次数"), List.of("月卡", "12")),
                DocumentBlock.tableRow(List.of("套餐", "次数"), List.of("年卡", "120"))));

        List<Chunk> chunks = chunker.chunk(document, "doc-2", ChunkStrategy.RECORD);

        assertThat(chunks).hasSize(2);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.text()).contains("套餐", "次数"));
    }

    @Test
    void usesStableStrategyAwareChunkIds() {
        ParsedDocument document = new ParsedDocument(List.of(DocumentBlock.paragraph("训练说明")));
        List<Chunk> first = chunker.chunk(document, "doc-3", ChunkStrategy.KNOWLEDGE);
        List<Chunk> second = chunker.chunk(document, "doc-3", ChunkStrategy.KNOWLEDGE);

        assertThat(first).extracting(Chunk::id).containsExactlyElementsOf(second.stream().map(Chunk::id).toList());
        assertThat(first.get(0).id()).isNotEqualTo(chunker.chunk(document, "doc-3", ChunkStrategy.GENERAL).get(0).id());
    }

    @Test
    void generalStrategyKeepsSectionsOutOfOneLargeWindow() {
        ParsedDocument document = new ParsedDocument(List.of(
                DocumentBlock.title("训练安全"),
                DocumentBlock.paragraph("先热身，使用稳定重量。"),
                DocumentBlock.title("营养边界"),
                DocumentBlock.paragraph("疾病相关问题应咨询专业人士。")));

        List<Chunk> chunks = chunker.chunk(document, "doc-4", ChunkStrategy.GENERAL);

        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0).text()).contains("训练安全").doesNotContain("营养边界");
        assertThat(chunks.get(1).text()).contains("营养边界").doesNotContain("训练安全");
    }

    @Test
    void regulationStrategySplitsOneChunkPerClauseAndMergesSubItems() {
        ParsedDocument document = new ParsedDocument(List.of(
                DocumentBlock.title("第一章 总则"),
                DocumentBlock.paragraph("为规范会员管理，特制定本制度。"),
                DocumentBlock.clause("第一条 本制度适用于全体注册会员。", 2),
                DocumentBlock.clause("第二条 会员应遵守场地秩序。", 2),
                DocumentBlock.clause("（一）保持训练区安静。", 3),
                DocumentBlock.clause("（二）使用器械后归位。", 3)));

        List<Chunk> chunks = chunker.chunk(document, "doc-5", ChunkStrategy.REGULATION);

        // 总则一段 + 第一条 + 第二条（含两款）
        assertThat(chunks).hasSize(3);
        assertThat(chunks.get(0).text()).startsWith("【第一章 总则】").contains("特制定本制度");
        assertThat(chunks.get(1).text()).startsWith("【第一章 总则 · 第一条 本制度适用于全体注册会员】");
        assertThat(chunks.get(2).text()).contains("第二条", "（一）", "（二）");
    }

    @Test
    void regulationStrategyKeepsLabelOnEveryPieceOfLongClause() {
        StringBuilder longClause = new StringBuilder("第五条 会员退卡应按以下流程办理。");
        for (int i = 0; i < 40; i++) {
            longClause.append("申请人须携带有效证件前往前台填写申请表，工作人员核验身份后进入审核环节。");
        }
        ParsedDocument document = new ParsedDocument(List.of(
                DocumentBlock.title("第三章 会员卡管理"),
                DocumentBlock.clause(longClause.toString(), 2)));

        List<Chunk> chunks = chunker.chunk(document, "doc-6", ChunkStrategy.REGULATION);

        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> {
            assertThat(chunk.text()).startsWith("【第三章 会员卡管理 · 第五条 会员退卡应按以下流程办理】");
            assertThat(chunk.text().length()).isLessThanOrEqualTo(1200);
        });
    }

    @Test
    void regulationStrategyFallsBackToSectionGroupingWithoutClauses() {
        ParsedDocument document = new ParsedDocument(List.of(
                DocumentBlock.title("安全须知"),
                DocumentBlock.paragraph("训练前必须热身。")));

        List<Chunk> chunks = chunker.chunk(document, "doc-7", ChunkStrategy.REGULATION);

        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).text()).doesNotStartWith("【");
    }
}
