package com.gymmind.knowledge.infrastructure.parser.normalize;

import com.gymmind.knowledge.infrastructure.parser.DocumentBlock;
import com.gymmind.knowledge.infrastructure.parser.ParsedDocument;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** 从 DOCX 提取结构化文档块（供 MD 渲染与旧版解析复用） */
public final class DocxStructureParser {

    private DocxStructureParser() {
    }

    public static ParsedDocument parse(byte[] bytes) {
        List<DocumentBlock> blocks = new ArrayList<>();
        try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            doc.getBodyElements().forEach(body -> {
                if (body instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph) {
                    String value = paragraph.getText();
                    if (value == null || value.isBlank()) {
                        return;
                    }
                    String style = paragraph.getStyle();
                    if (style != null && style.toLowerCase(Locale.ROOT).startsWith("heading")) {
                        int level = headingLevel(style);
                        blocks.add(DocumentBlock.title(value, level));
                    } else if (paragraph.getNumID() != null) {
                        int level = paragraph.getNumIlvl() == null ? 0 : paragraph.getNumIlvl().intValue();
                        blocks.add(DocumentBlock.listItem(value, level));
                    } else if (isLikelySectionTitle(value)) {
                        blocks.add(DocumentBlock.title(value, 1));
                    } else {
                        blocks.add(DocumentBlock.paragraph(value));
                    }
                } else if (body instanceof XWPFTable table) {
                    if (table.getNumberOfRows() == 0) {
                        return;
                    }
                    List<String> headers = cells(table.getRow(0));
                    if (headers.stream().allMatch(String::isBlank)) {
                        return;
                    }
                    for (int i = 1; i < table.getNumberOfRows(); i++) {
                        List<String> values = cells(table.getRow(i));
                        if (values.stream().anyMatch(s -> !s.isBlank())) {
                            blocks.add(DocumentBlock.tableRow(headers, values));
                        }
                    }
                }
            });
            return new ParsedDocument(blocks);
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid DOCX", e);
        }
    }

    private static int headingLevel(String style) {
        if (style == null || style.isBlank()) {
            return 1;
        }
        String digits = style.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 1;
        }
        try {
            return Math.max(1, Math.min(6, Integer.parseInt(digits)));
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private static boolean isLikelySectionTitle(String value) {
        String text = value == null ? "" : value.trim();
        if (text.length() < 2 || text.length() > 48) {
            return false;
        }
        return !text.matches(".*[。！？!?；;：:,，].*")
                && !text.endsWith(".")
                && !text.endsWith("．");
    }

    private static List<String> cells(XWPFTableRow row) {
        return row.getTableCells().stream().map(cell -> cell.getText().trim()).toList();
    }
}
