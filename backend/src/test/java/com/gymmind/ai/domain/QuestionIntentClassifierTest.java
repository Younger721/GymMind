package com.gymmind.ai.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionIntentClassifierTest {
    private final QuestionIntentClassifier classifier = new QuestionIntentClassifier();

    @Test void classifiesTrainingQuestions() {
        assertThat(classifier.classify("帮我制定今天的训练计划")).isEqualTo(QuestionIntent.TRAINING);
    }

    @Test void classifiesNutritionQuestions() {
        assertThat(classifier.classify("今天应该摄入多少蛋白质和热量")).isEqualTo(QuestionIntent.NUTRITION);
    }

    @Test void classifiesProgressQuestions() {
        assertThat(classifier.classify("帮我看看最近体重和训练进度")).isEqualTo(QuestionIntent.PROGRESS);
    }

    @Test void fallsBackToGeneralForUnknownOrBlankQuestions() {
        assertThat(classifier.classify("随便聊聊")).isEqualTo(QuestionIntent.GENERAL);
        assertThat(classifier.classify("  ")).isEqualTo(QuestionIntent.GENERAL);
        assertThat(classifier.classify(null)).isEqualTo(QuestionIntent.GENERAL);
    }
}
