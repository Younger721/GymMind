package com.gymmind.knowledge.infrastructure.parser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** 规章制度结构识别：只有呈现规章特征的文档才升级「章/条/款项」块。 */
class RegulationStructureUpgradeTest {

    private final DocumentParserRegistry registry = DocumentParserRegistry.standard();

    @Test
    void upgradesClauseLinesForPlainTextDocuments() {
        String text = String.join("\n",
                "健身房会员管理制度",
                "第一章 总则",
                "第一条 为规范会员管理，特制定本制度。",
                "（一）安全第一。",
                "（二）爱护器材。",
                "第二条 本制度适用于全体注册会员。",
                "以上解释权归本店所有。");
        // 解析器固定按 UTF-8 解码，这里必须显式指定字符集，避免随 JVM 默认编码产生乱码
        ParsedDocument parsed = registry.parseStructured("rules.txt", "text/plain",
                text.getBytes(StandardCharsets.UTF_8));

        assertThat(parsed.blocks()).hasSize(7);
        assertThat(parsed.blocks().get(1).type()).isEqualTo(DocumentBlockType.TITLE);
        assertThat(parsed.blocks().get(2).type()).isEqualTo(DocumentBlockType.CLAUSE);
        assertThat(parsed.blocks().get(2).level()).isEqualTo(2);
        assertThat(parsed.blocks().get(3).type()).isEqualTo(DocumentBlockType.CLAUSE);
        assertThat(parsed.blocks().get(3).level()).isEqualTo(3);
        assertThat(parsed.blocks().get(5).type()).isEqualTo(DocumentBlockType.CLAUSE);
        assertThat(parsed.blocks().get(6).type()).isEqualTo(DocumentBlockType.PARAGRAPH);
    }

    @Test
    void keepsOrdinaryTextUntouched() {
        String text = String.join("\n",
                "今天课程安排说明",
                "第一条线路是瑜伽课",
                "普通说明文字没有规章特征");
        ParsedDocument parsed = registry.parseStructured("notes.txt", "text/plain",
                text.getBytes(StandardCharsets.UTF_8));

        assertThat(parsed.blocks()).allSatisfy(block ->
                assertThat(block.type()).isEqualTo(DocumentBlockType.PARAGRAPH));
    }

    @Test
    void upgradesMarkdownParagraphsThatContainClauseNumbers() {
        String text = String.join("\n",
                "# 会员管理细则",
                "第一章 适用范围",
                "第一条 本细则自发布之日起执行。",
                "第二条 会员入会需签署知情同意书。");
        ParsedDocument parsed = registry.parseStructured("rules.md", "text/markdown",
                text.getBytes(StandardCharsets.UTF_8));

        assertThat(parsed.blocks().get(1).type()).isEqualTo(DocumentBlockType.TITLE);
        assertThat(parsed.blocks().get(2).type()).isEqualTo(DocumentBlockType.CLAUSE);
        assertThat(parsed.blocks().get(3).type()).isEqualTo(DocumentBlockType.CLAUSE);
    }
}
