package com.gymmind.knowledge.infrastructure.parser;

/** Structural units preserved while parsing a source document. */
public enum DocumentBlockType {
    TITLE,
    /** 规章制度中的条款单元（第X条 / 第X款 / 项），是条款切片的原子边界 */
    CLAUSE,
    PARAGRAPH,
    LIST_ITEM,
    TABLE_ROW
}
