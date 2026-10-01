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
}
