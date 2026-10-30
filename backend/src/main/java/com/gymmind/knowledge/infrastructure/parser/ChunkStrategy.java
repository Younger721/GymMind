package com.gymmind.knowledge.infrastructure.parser;

public enum ChunkStrategy {
    PROCEDURE,
    KNOWLEDGE,
    FAQ_RULE,
    RECORD,
    /** 规章制度：按「章 > 条」条款边界切片，一款/一项并入所属条 */
    REGULATION,
    GENERAL
}
