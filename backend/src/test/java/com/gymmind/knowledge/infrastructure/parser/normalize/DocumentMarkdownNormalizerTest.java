package com.gymmind.knowledge.infrastructure.parser.normalize;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentMarkdownNormalizerTest {

    @Test
    void convertsHtmlToMarkdownHeadings() {
        var normalizer = DocumentMarkdownNormalizer.create(true);
        byte[] html = "<h1>Title</h1><p>Body</p>".getBytes(StandardCharsets.UTF_8);
        assertThat(normalizer.shouldConvert("page.html", "text/html")).isTrue();
        String md = normalizer.toMarkdown("page.html", "text/html", html);
        assertThat(md).contains("# Title").contains("Body");
    }

    @Test
    void skipsMarkdownAndPlainText() {
        var normalizer = DocumentMarkdownNormalizer.create(true);
        assertThat(normalizer.shouldConvert("a.md", "text/markdown")).isFalse();
        assertThat(normalizer.shouldConvert("a.txt", "text/plain")).isFalse();
    }

    @Test
    void disabledSkipsConversion() {
        var normalizer = DocumentMarkdownNormalizer.create(false);
        assertThat(normalizer.shouldConvert("a.pdf", "application/pdf")).isFalse();
    }
}
