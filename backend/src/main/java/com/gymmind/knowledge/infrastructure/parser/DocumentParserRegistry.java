package com.gymmind.knowledge.infrastructure.parser;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;
import java.io.ByteArrayInputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

public final class DocumentParserRegistry {
    private static final Pattern TAG = Pattern.compile("<[^>]+>");
    private static final Pattern SCRIPT = Pattern.compile("(?is)<script[^>]*>.*?</script>");

    private DocumentParserRegistry() {}

    public static DocumentParserRegistry standard() { return new DocumentParserRegistry(); }

    public String parse(String fileName, String contentType, byte[] bytes) {
        if (bytes == null || bytes.length == 0) throw new IllegalArgumentException("document is empty");
        String name = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (type.contains("text/plain") || name.endsWith(".txt")) return text(bytes);
        if (type.contains("markdown") || name.endsWith(".md")) return markdown(bytes);
        if (type.contains("text/html") || name.endsWith(".html") || name.endsWith(".htm")) return html(bytes);
        if (type.contains("pdf") || name.endsWith(".pdf")) return pdf(bytes);
        if (type.contains("wordprocessingml") || name.endsWith(".docx")) return docx(bytes);
        throw new IllegalArgumentException("unsupported document type");
    }

    private static String text(byte[] bytes) { return new String(bytes, StandardCharsets.UTF_8).trim(); }
    private static String markdown(byte[] bytes) {
        return text(bytes).replaceAll("(?m)^#{1,6}\\s*", "").replace("**", "").replace("__", "");
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
    private static String docx(byte[] bytes) {
        try (var doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            return doc.getParagraphs().stream().map(p -> p.getText()).filter(s -> !s.isBlank()).reduce((a,b) -> a + "\n" + b).orElse("").trim();
        } catch (Exception e) { throw new IllegalArgumentException("invalid DOCX", e); }
    }
}
