package com.gymmind.ai.application;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeAnswerFormatterTest {

    @Test
    void prefixesDocumentAndClauseFromCitation() {
        var citation = new SourceCitation(
                "chunk-1",
                "14",
                "发生训练区重伤或监管抽查不合格事件，店管理员须在 2 小时内通过平台上报事件摘要。",
                "平台管理员门店运营与合规发布手册.docx",
                1,
                "GENERAL_NOTICE",
                null,
                null,
                null);
        String formatted = KnowledgeAnswerFormatter.apply(
                "店管理员须在2小时内通过平台上报事件摘要，并在72小时内提交完整《训练区事件记录表》扫描件。",
                List.of(citation));
        assertThat(formatted).startsWith("根据《平台管理员门店运营与合规发布手册》相关条款可得，");
        assertThat(formatted).contains("2小时内");
    }

    @Test
    void keepsExistingPrefix() {
        assertThat(KnowledgeAnswerFormatter.apply("根据《手册》PLAT-OPS-001可得，结论。", List.of()))
                .isEqualTo("根据《手册》PLAT-OPS-001可得，结论。");
    }
}
