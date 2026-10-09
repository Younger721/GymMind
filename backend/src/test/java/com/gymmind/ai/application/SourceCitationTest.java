package com.gymmind.ai.application;

import com.gymmind.ai.domain.ContextSegment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceCitationTest {
    @Test
    void citationUsesACompactExcerptAroundTheQuestion() {
        String text = "前置章节。".repeat(80) + "新手进行力量训练时应先热身并使用稳定重量。" + "后置章节。".repeat(80);

        SourceCitation citation = SourceCitation.from(
                new ContextSegment("chunk-1", "doc-1", text), "新手力量训练要注意什么");

        assertThat(citation.excerpt()).hasSizeLessThanOrEqualTo(380);
        assertThat(citation.excerpt()).contains("力量训练", "热身");
        assertThat(citation.excerpt()).doesNotContain("前置章节。".repeat(80));
    }
}
