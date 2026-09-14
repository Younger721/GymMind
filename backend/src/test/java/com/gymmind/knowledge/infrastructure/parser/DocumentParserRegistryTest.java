package com.gymmind.knowledge.infrastructure.parser;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;

class DocumentParserRegistryTest {
    private final DocumentParserRegistry registry = DocumentParserRegistry.standard();

    @Test void parsesPlainTextAndMarkdownAsReadableText() {
        assertThat(registry.parse("notes.txt", "text/plain", "hello\nworld".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("hello\nworld");
        assertThat(registry.parse("notes.md", "text/markdown", "# Title\n**body**".getBytes(StandardCharsets.UTF_8)))
                .contains("Title", "body").doesNotContain("**");
    }

    @Test void stripsHtmlMarkupWithoutLeakingScripts() {
        String text = registry.parse("page.html", "text/html", "<h1>Title</h1><script>x()</script><p>Body</p>".getBytes(StandardCharsets.UTF_8));
        assertThat(text).contains("Title", "Body").doesNotContain("x()", "<h1>");
    }

    @Test void rejectsEmptyAndUnsupportedDocuments() {
        assertThatThrownBy(() -> registry.parse("x.txt", "text/plain", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> registry.parse("x.zip", "application/zip", new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
