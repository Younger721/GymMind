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

    @Test
    void extractsClauseLabelFromBreadcrumbText() {
        assertThat(SourceCitation.chunkLabel("【第三章 会员卡管理 · 第12条 退卡规定】会员退卡应凭有效证件办理。"))
                .isEqualTo("第三章 会员卡管理 · 第12条 退卡规定");
    }

    @Test
    void returnsNullWhenTextHasNoBreadcrumb() {
        assertThat(SourceCitation.chunkLabel("普通训练说明，没有面包屑前缀。")).isNull();
        assertThat(SourceCitation.chunkLabel(null)).isNull();
    }

    @Test
    void fromCarriesChunkLabelWhileKeepingExcerptIntact() {
        String text = "【第一章 总则 · 第一条 适用范围】本制度适用于全体注册会员。";
        var citation = SourceCitation.from(new ContextSegment("c1", "9", text), "适用范围");

        assertThat(citation.chunkLabel()).isEqualTo("第一章 总则 · 第一条 适用范围");
        assertThat(citation.excerpt()).isEqualTo(text);
    }
}
