package com.gymmind.knowledge.infrastructure.parser;

import com.gymmind.knowledge.infrastructure.parser.normalize.DocxStructureParser;
import com.gymmind.knowledge.infrastructure.parser.normalize.DocumentMarkdownNormalizer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.io.ByteArrayInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

public final class DocumentParserRegistry {
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT = Pattern.compile("(?is)<script[^>]*>.*?</script>");

    private final DocumentMarkdownNormalizer markdownNormalizer;

    private DocumentParserRegistry(DocumentMarkdownNormalizer markdownNormalizer) {
        this.markdownNormalizer = markdownNormalizer == null
                ? DocumentMarkdownNormalizer.create(true) : markdownNormalizer;
    }

    public static DocumentParserRegistry standard() {
        return new DocumentParserRegistry(DocumentMarkdownNormalizer.create(true));
    }

    public static DocumentParserRegistry withNormalizer(DocumentMarkdownNormalizer markdownNormalizer) {
        return new DocumentParserRegistry(markdownNormalizer);
    }

    public String parse(String fileName, String contentType, byte[] bytes) {
        return parseStructured(fileName, contentType, bytes).plainText();
    }

    public ParsedDocument parseStructured(String fileName, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("document is empty");
        }
        if (markdownNormalizer.shouldConvert(fileName, contentType)) {
            return markdownStructured(markdownNormalizer.toMarkdownBytes(fileName, contentType, bytes));
        }
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("text/plain") || name.endsWith(".txt")) {
            return lines(text(bytes));
        }
        if (type.contains("markdown") || name.endsWith(".md")) {
            return markdownStructured(bytes);
        }
        if (type.contains("text/html") || name.endsWith(".html") || name.endsWith(".htm")) {
            return lines(html(bytes));
        }
        if (type.contains("pdf") || name.endsWith(".pdf")) {
            return lines(pdf(bytes));
        }
        if (type.contains("wordprocessingml") || name.endsWith(".docx")) {
            return DocxStructureParser.parse(bytes);
        }
        throw new IllegalArgumentException("unsupported document type");
    }

    private static String text(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8).trim(); }

    private static ParsedDocument markdownStructured(byte[] bytes) {
        List<DocumentBlock> blocks = new ArrayList<>();
        for (String raw : new String(bytes, StandardCharsets.UTF_8).split("\\R")) {
            String line = raw.trim();
            if (line.isBlank()) {
                continue;
            }
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
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid PDF", e);
        }
    }

    private static ParsedDocument lines(String value) {
        List<DocumentBlock> blocks = new ArrayList<>();
        for (String line : value.split("\\R")) {
            if (!line.isBlank()) {
                blocks.add(DocumentBlock.paragraph(line));
            }
        }
        if (blocks.isEmpty() && !value.isBlank()) {
            blocks.add(DocumentBlock.paragraph(value));
        }
        return new ParsedDocument(blocks);
    }
}
