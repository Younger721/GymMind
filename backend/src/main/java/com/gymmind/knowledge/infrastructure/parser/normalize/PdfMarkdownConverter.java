package com.gymmind.knowledge.infrastructure.parser.normalize;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

/** PDF 抽取纯文本并按段落转为 Markdown */
public final class PdfMarkdownConverter {

    private PdfMarkdownConverter() {
    }

    public static String convert(byte[] bytes) {
        try (var doc = Loader.loadPDF(bytes)) {
            String raw = new PDFTextStripper().getText(doc).trim();
            if (raw.isBlank()) {
                return "";
            }
            String[] paragraphs = raw.split("\\R\\s*\\R+");
            StringBuilder out = new StringBuilder();
            for (String paragraph : paragraphs) {
                String text = paragraph.replaceAll("\\s+", " ").trim();
                if (!text.isBlank()) {
                    out.append(text).append("\n\n");
                }
            }
            if (out.isEmpty()) {
                return raw.replaceAll("\\s+", " ").trim();
            }
            return out.toString().trim();
        } catch (Exception e) {
            throw new IllegalArgumentException("invalid PDF", e);
        }
    }
}
