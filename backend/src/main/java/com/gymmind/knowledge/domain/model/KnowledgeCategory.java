package com.gymmind.knowledge.domain.model;

/**
 * 健身 RAG 知识库业务分类（映射文档 {@code category} 字段，存大写字符串）。
 */
public enum KnowledgeCategory {
    /** 健身动作规范 */
    ACTION_STANDARD,
    /** 训练基础知识 */
    TRAINING_BASICS,
    /** 器械使用说明 */
    EQUIPMENT_GUIDE,
    /** 健身房课程介绍 */
    COURSE_INTRO,
    /** 饮食基础知识 */
    NUTRITION_BASICS,
    /** 健身房规章制度 */
    GYM_RULES,
    /** 常见健身问题 */
    FAQ,
    /** 未归类 */
    GENERAL;

    public String code() {
        return name();
    }

    public static KnowledgeCategory fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return GENERAL;
        }
    }
}
