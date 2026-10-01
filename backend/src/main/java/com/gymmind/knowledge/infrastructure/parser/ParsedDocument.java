package com.gymmind.knowledge.infrastructure.parser;

import java.util.List;

/** Parsed document with source order and structural boundaries retained. */
public record ParsedDocument(List<DocumentBlock> blocks) {
    public ParsedDocument {
        blocks = blocks == null ? List.of() : List.copyOf(blocks);
    }

    public String plainText() {
        return blocks.stream().map(DocumentBlock::text).filter(s -> !s.isBlank()).reduce((a, b) -> a + "\n" + b).orElse("");
    }
}
