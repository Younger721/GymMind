package com.gymmind.knowledge.infrastructure.parser.normalize;

import com.gymmind.knowledge.infrastructure.parser.DocumentBlock;
import com.gymmind.knowledge.infrastructure.parser.DocumentBlockType;
import com.gymmind.knowledge.infrastructure.parser.ParsedDocument;

import java.util.List;
import java.util.Objects;

/** 将结构化文档块渲染为 Markdown 文本 */
public final class DocumentBlocksToMarkdown {

    private DocumentBlocksToMarkdown() {
    }

    public static String render(ParsedDocument document) {
        if (document == null || document.blocks().isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        List<String> activeTableHeaders = null;
        for (DocumentBlock block : document.blocks()) {
            if (block.type() != DocumentBlockType.TABLE_ROW) {
                activeTableHeaders = null;
            }
            switch (block.type()) {
                case TITLE -> appendTitle(out, block);
                case PARAGRAPH -> appendParagraph(out, block.text());
                case LIST_ITEM -> appendListItem(out, block);
                case TABLE_ROW -> activeTableHeaders = appendTableRow(out, block, activeTableHeaders);
                default -> appendParagraph(out, block.text());
            }
        }
        return out.toString().trim();
    }

    private static void appendTitle(StringBuilder out, DocumentBlock block) {
        int level = Math.max(1, Math.min(6, block.level()));
        out.append("#".repeat(level)).append(' ').append(block.text()).append("\n\n");
    }

    private static void appendParagraph(StringBuilder out, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        out.append(text.trim()).append("\n\n");
    }

    private static void appendListItem(StringBuilder out, DocumentBlock block) {
        String indent = "  ".repeat(Math.max(0, block.level()));
        out.append(indent).append("- ").append(block.text()).append('\n');
    }

    private static List<String> appendTableRow(StringBuilder out, DocumentBlock block, List<String> activeHeaders) {
        List<String> headers = block.hasTableHeaders() ? block.tableHeaders() : activeHeaders;
        if (headers == null || headers.isEmpty()) {
            appendParagraph(out, block.text());
            return null;
        }
        if (!Objects.equals(headers, activeHeaders)) {
            out.append('|').append(String.join("|", headers)).append("|\n");
            for (int i = 0; i < headers.size(); i++) {
                out.append("---|");
            }
            out.append('\n');
        }
        String[] parts = block.text().split("\n", 2);
        String rowLine = parts.length > 1 ? parts[1] : parts[0];
        List<String> cells = List.of(rowLine.split("\\|", -1));
        if (cells.size() == headers.size()) {
            out.append('|').append(String.join("|", cells)).append("|\n");
        } else {
            out.append('|').append(rowLine.replace("\n", " ")).append("|\n");
        }
        return headers;
    }
}
