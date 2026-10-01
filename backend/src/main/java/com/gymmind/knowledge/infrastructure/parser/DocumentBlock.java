package com.gymmind.knowledge.infrastructure.parser;

import java.util.List;
import java.util.Objects;

/** A normalized document block used by structure-aware chunkers. */
public record DocumentBlock(DocumentBlockType type, int level, String text, List<String> tableHeaders) {
    public DocumentBlock {
        type = Objects.requireNonNull(type, "type");
        text = text == null ? "" : text.trim();
        if (text.isBlank()) {
            throw new IllegalArgumentException("document block text is required");
        }
        level = Math.max(0, level);
        tableHeaders = tableHeaders == null ? List.of() : List.copyOf(tableHeaders);
    }

    public static DocumentBlock title(String text) {
        return new DocumentBlock(DocumentBlockType.TITLE, 1, text, List.of());
    }

    public static DocumentBlock title(String text, int level) {
        return new DocumentBlock(DocumentBlockType.TITLE, level, text, List.of());
    }

    public static DocumentBlock paragraph(String text) {
        return new DocumentBlock(DocumentBlockType.PARAGRAPH, 0, text, List.of());
    }

    public static DocumentBlock listItem(String text, int level) {
        return new DocumentBlock(DocumentBlockType.LIST_ITEM, level, text, List.of());
    }

    public static DocumentBlock tableRow(List<String> headers, List<String> values) {
        List<String> safeHeaders = headers == null ? List.of() : headers;
        String headerText = String.join(" | ", safeHeaders);
        String valueText = String.join(" | ", values == null ? List.of() : values);
        String text = headerText.isBlank() ? valueText : headerText + "\n" + valueText;
        return new DocumentBlock(DocumentBlockType.TABLE_ROW, 0, text, safeHeaders);
    }

    public boolean hasTableHeaders() {
        return !tableHeaders.isEmpty();
    }
}
