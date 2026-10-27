package com.gymmind.knowledge.infrastructure.parser;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StructuredDocumentParserTest {
    @Test
    void preservesDocxHeadingsListsAndTableRows() throws Exception {
        byte[] bytes;
        try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            XWPFParagraph heading = document.createParagraph();
            heading.setStyle("Heading1");
            heading.createRun().setText("深蹲训练");
            XWPFParagraph step = document.createParagraph();
            step.setNumID(BigInteger.ONE);
            step.createRun().setText("保持背部中立");
            XWPFTable table = document.createTable(2, 2);
            XWPFTableRow header = table.getRow(0);
            header.getCell(0).setText("套餐");
            header.getCell(1).setText("次数");
            XWPFTableRow row = table.getRow(1);
            row.getCell(0).setText("月卡");
            row.getCell(1).setText("12");
            document.write(output);
            bytes = output.toByteArray();
        }

        ParsedDocument parsed = DocumentParserRegistry.standard()
                .parseStructured("guide.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes);

        assertThat(parsed.blocks()).extracting(DocumentBlock::type)
                .contains(DocumentBlockType.TITLE, DocumentBlockType.LIST_ITEM);
        assertThat(parsed.plainText()).contains("深蹲训练", "套餐", "月卡", "12");
    }

    @Test
    void parsesPlainTextIntoOrderedParagraphBlocks() {
        ParsedDocument parsed = DocumentParserRegistry.standard()
                .parseStructured("guide.txt", "text/plain", "标题\n第一段\n\n第二段".getBytes(StandardCharsets.UTF_8));

        assertThat(parsed.blocks()).extracting(DocumentBlock::text)
                .containsExactly("标题", "第一段", "第二段");
        assertThat(parsed.blocks()).allMatch(block -> block.type() == DocumentBlockType.PARAGRAPH);
    }

    @Test
    void recognizesShortUnstyledDocxSectionNamesAsTitles() throws Exception {
        byte[] bytes;
        try (var document = new XWPFDocument(); var output = new ByteArrayOutputStream()) {
            document.createParagraph().createRun().setText("训练安全");
            document.createParagraph().createRun().setText("先热身并使用稳定重量。");
            document.write(output);
            bytes = output.toByteArray();
        }

        ParsedDocument parsed = DocumentParserRegistry.standard()
                .parseStructured("guide.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", bytes);

        assertThat(parsed.blocks().get(0).type()).isEqualTo(DocumentBlockType.TITLE);
    }
}
