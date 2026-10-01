package com.gymmind.knowledge.infrastructure.parser;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.ByteArrayInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;

public final class DocumentParserRegistry {
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT = Pattern.compile("(?is)<script[^>]*>.*?</script>");

    private DocumentParserRegistry() {}

    public static DocumentParserRegistry standard() { return new DocumentParserRegistry(); }

    public String parse(String fileName, String contentType, byte[] bytes) {
        return parseStructured(fileName, contentType, bytes).plainText();
    }

    public ParsedDocument parseStructured(String fileName, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("document is empty");
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("text/plain") || name.endsWith(".txt")) return lines(text(bytes));
        if (type.contains("markdown") || name.endsWith(".md")) return markdownStructured(bytes);
        if (type.contains("text/html") || name.endsWith(".html") || name.endsWith(".htm")) return lines(html(bytes));
        if (type.contains("pdf") || name.endsWith(".pdf")) return lines(pdf(bytes));
        if (type.contains("wordprocessingml") || name.endsWith(".docx")) return docxStructured(bytes);
        throw new IllegalArgumentException("unsupported document type");
    }

    private static String text(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8).trim(); }
    private static String markdown(byte[] bytes) {
        return text(bytes).replaceAll("(?m)^#{1,6}\\s*", "").replace("**", "").replace("__", "");
    }
    private static ParsedDocument markdownStructured(byte[] bytes) {
        List<DocumentBlock> blocks = new ArrayList<>();
        for (String raw : new String(bytes, StandardCharsets.UTF_8).split("\\R")) {
            String line = raw.trim();
            if (line.isBlank()) continue;
            var heading = Pattern.compile("^(#{1,6})\\s+(.+)$").matcher(line);
            if (heading.matches()) {
                blocks.add(DocumentBlock.title(cleanMarkdown(heading.group(2)), heading.group(1).length()));
            } else if (line.matches("^([-*+] |\\d+[.)] ).+$")) {
                blocks.add(DocumentBlock.listItem(cleanMarkdown(line.replaceFirst("^([-*+] |\\d+[.)] )", "")), 0));
            } else {
                blocks.add(DocumentBlock.paragraph(cleanMarkdown(line)));
            }
        }
        return new ParsedDocument(blocks);
    }
    private static String cleanMarkdown(String value) {
        return value.replace("**", "").replace("__", "").replaceAll("[`*_]", "").trim();
    }
    private static String html(byte[] bytes) {
        String value = new String(bytes, StandardCharsets.UTF_8);
        value = SCRIPT.matcher(value).replaceAll(" ");
        return TAG.matcher(value).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }
    private static String pdf(byte[] bytes) {
        try (var doc = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(doc).trim();
        } catch (Exception e) { throw new IllegalArgumentException("invalid PDF", e); }
    }
    private static ParsedDocument docxStructured(byte[] bytes) {
        List<DocumentBlock> blocks = new ArrayList<>();
        try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            doc.getBodyElements().forEach(body -> {
                if (body instanceof org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph) {
                    String value = paragraph.getText();
                    if (value == null || value.isBlank()) return;
                    String style = paragraph.getStyle();
                    if (style != null && style.toLowerCase(Locale.ROOT).startsWith("heading")) {
                        int level = headingLevel(style);
                        blocks.add(DocumentBlock.title(value, level));
                    } else if (paragraph.getNumID() != null) {
                        int level = paragraph.getNumIlvl() == null ? 0 : paragraph.getNumIlvl().intValue();
                        blocks.add(DocumentBlock.listItem(value, level));
                    } else {
                        blocks.add(DocumentBlock.paragraph(value));
                    }
                } else if (body instanceof XWPFTable table) {
                    if (table.getNumberOfRows() == 0) return;
                    List<String> headers = cells(table.getRow(0));
                    if (headers.stream().allMatch(String::isBlank)) return;
                    for (int i = 1; i < table.getNumberOfRows(); i++) {
                        List<String> values = cells(table.getRow(i));
                        if (values.stream().anyMatch(s -> !s.isBlank())) blocks.add(DocumentBlock.tableRow(headers, values));
                    }
                }
            });
            return new ParsedDocument(blocks);
        } catch (Exception e) { throw new IllegalArgumentException("invalid DOCX", e); }
    }

    private static ParsedDocument lines(String value) {
        List<DocumentBlock> blocks = new ArrayList<>();
        for (String line : value.split("\\R")) {
            if (!line.isBlank()) blocks.add(DocumentBlock.paragraph(line));
        }
        if (blocks.isEmpty() && !value.isBlank()) blocks.add(DocumentBlock.paragraph(value));
        return new ParsedDocument(blocks);
    }

    private static int headingLevel(String style) {
        String digits = style.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 1;
        try { return Math.max(1, Math.min(6, Integer.parseInt(digits))); }
        catch (NumberFormatException ignored) { return 1; }
    }

    private static List<String> cells(org.apache.poi.xwpf.usermodel.XWPFTableRow row) {
        return row.getTableCells().stream().map(cell -> cell.getText().trim()).toList();
    }

    private static String docx(byte[] bytes) {
        return docxStructured(bytes).plainText();
    }
}
