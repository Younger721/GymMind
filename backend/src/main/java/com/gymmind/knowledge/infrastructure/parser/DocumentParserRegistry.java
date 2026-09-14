package com.gymmind.knowledge.infrastructure.parser;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

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
}
