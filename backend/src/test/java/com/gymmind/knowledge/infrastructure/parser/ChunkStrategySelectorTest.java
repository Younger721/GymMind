package com.gymmind.knowledge.infrastructure.parser;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;

class ChunkStrategySelectorTest {
    private final ChunkStrategySelector selector = new ChunkStrategySelector();

    @Test
    void selectsProcedureForTrainingSteps() {
        assertThat(selector.select("动作规范", "squat-guide.docx", "步骤一：站稳\n步骤二：下蹲"))
                .isEqualTo(ChunkStrategy.PROCEDURE);
    }

    @Test
    void selectsFaqForRulesAndQuestions() {
        assertThat(selector.select("FAQ", "faq.md", "问：会员可以取消课程吗？\n答：开课前可以"))
                .isEqualTo(ChunkStrategy.FAQ_RULE);
    }

    @Test
    void selectsRecordForCourseOrPackageTables() {
        assertThat(selector.select("课程", "schedule.docx", "课程|教练|时间\n瑜伽|李教练|周一"))
                .isEqualTo(ChunkStrategy.RECORD);
    }

    @Test
    void fallsBackToGeneralForUnknownContent() {
        assertThat(selector.select("其他", "notes.txt", "今天有一段普通说明"))
                .isEqualTo(ChunkStrategy.GENERAL);
    }

    @Test
    void acceptsOnlyHighConfidenceStrictModelClassification() {
        var model = mock(com.gymmind.ai.application.ChatModelGateway.class);
        when(model.complete(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn("{\"strategy\":\"KNOWLEDGE\",\"confidence\":0.91}");

        assertThat(new ChunkStrategySelector(model).select("其他", "notes.txt", "普通说明"))
                .isEqualTo(ChunkStrategy.KNOWLEDGE);
    }

    @Test
    void fallsBackWhenModelResponseIsInvalidOrLowConfidence() {
        var model = mock(com.gymmind.ai.application.ChatModelGateway.class);
        when(model.complete(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn("not-json")
                .thenReturn("{\"strategy\":\"RECORD\",\"confidence\":0.2}");
        var withModel = new ChunkStrategySelector(model);

        assertThat(withModel.select("其他", "notes.txt", "普通说明"))
                .isEqualTo(ChunkStrategy.GENERAL);
        assertThat(withModel.select("其他", "notes.txt", "普通说明"))
                .isEqualTo(ChunkStrategy.GENERAL);
    }
}
