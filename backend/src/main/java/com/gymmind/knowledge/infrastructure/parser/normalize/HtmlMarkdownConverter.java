package com.gymmind.knowledge.infrastructure.parser.normalize;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 将 HTML 转为简易 Markdown（块级标签为主） */
public final class HtmlMarkdownConverter {

    private static final Pattern SCRIPT = Pattern.compile("(?is)<script[^>]*>.*?</script>");
    private static final Pattern STYLE = Pattern.compile("(?is)<style[^>]*>.*?</style>");
    private static final Pattern HEADING = Pattern.compile("(?is)<h([1-6])[^>]*>(.*?)</h\\1>");
    private static final Pattern PARAGRAPH = Pattern.compile("(?is)<p[^>]*>(.*?)</p>");
    private static final Pattern LINE_BREAK = Pattern.compile("(?i)<br\\s*/?>");
    private static final Pattern LIST_ITEM = Pattern.compile("(?is)<li[^>]*>(.*?)</li>");
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    private HtmlMarkdownConverter() {
    }

    public static String convert(byte[] bytes) {
        String html = new String(bytes, StandardCharsets.UTF_8);
        html = SCRIPT.matcher(html).replaceAll(" ");
        html = STYLE.matcher(html).replaceAll(" ");
        html = LINE_BREAK.matcher(html).replaceAll("\n");
        StringBuilder out = new StringBuilder();
        Matcher heading = HEADING.matcher(html);
        while (heading.find()) {
            int level = Math.max(1, Math.min(6, Integer.parseInt(heading.group(1))));
            String text = stripTags(heading.group(2));
            if (!text.isBlank()) {
                out.append("#".repeat(level)).append(' ').append(text).append("\n\n");
            }
        }
        Matcher listItem = LIST_ITEM.matcher(html);
        while (listItem.find()) {
            String text = stripTags(listItem.group(1));
            if (!text.isBlank()) {
                out.append("- ").append(text).append('\n');
            }
        }
        if (!out.isEmpty() && out.charAt(out.length() - 1) == '\n') {
            out.append('\n');
        }
        Matcher paragraph = PARAGRAPH.matcher(html);
        while (paragraph.find()) {
            String text = stripTags(paragraph.group(1));
            if (!text.isBlank()) {
                out.append(text).append("\n\n");
            }
        }
        if (out.isEmpty()) {
            String plain = stripTags(html);
            if (!plain.isBlank()) {
                out.append(plain).append("\n\n");
            }
        }
        return out.toString().trim();
    }

    private static String stripTags(String value) {
        return TAG.matcher(value).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }
}
