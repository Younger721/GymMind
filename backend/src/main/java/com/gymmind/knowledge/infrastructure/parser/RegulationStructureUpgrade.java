package com.gymmind.knowledge.infrastructure.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 规章制度结构识别器：把纯文本行升级为「章 / 节 / 条 / 款项」结构块。
 *
 * <p>PDF、TXT 等解析出的行没有层级语义，规章制度“第X条”“（一）”这类编号是天然的切片边界。
 * 只有当文档确实呈现规章特征（至少两个“第X条”，或同时出现“第X章”与“第X条”）时才升级，
 * 避免把普通文档误判为规章。</p>
 */
public final class RegulationStructureUpgrade {

    /** 章：第X章，中文数字或阿拉伯数字 */
    private static final Pattern CHAPTER = Pattern.compile(
            "^第[一二三四五六七八九十百千零两0-9]+章([ 　:.：、\\-—].*)?$");
    /** 节：第X节 */
    private static final Pattern SECTION = Pattern.compile(
            "^第[一二三四五六七八九十百千零两0-9]+节([ 　:.：、\\-—].*)?$");
    /** 条：第X条，条号后必须跟分隔、正文或行尾 */
    private static final Pattern CLAUSE = Pattern.compile(
            "^第[一二三四五六七八九十百千零两0-9]+条([ 　:.：、\\-—].*)?$");
    /** 款/项：（一）、（1）、1. 、1、等编号短行 */
    private static final Pattern SUB_ITEM = Pattern.compile(
            "^（[一二三四五六七八九十0-9]+）.*$|^[（(]?[0-9一二三四五六七八九十]+[）)..、．]\\s*.*$");

    private RegulationStructureUpgrade() {
    }

    /**
     * 对解析结果做规章结构增强；非规章文档原样返回，已含条款块的文档不重复处理。
     */
    public static ParsedDocument enhance(ParsedDocument document) {
        if (document == null || document.blocks().isEmpty()) {
            return document;
        }
        boolean alreadyStructured = document.blocks().stream()
                .anyMatch(block -> block.type() == DocumentBlockType.CLAUSE);
        if (alreadyStructured || !looksLikeRegulation(document.blocks())) {
            return document;
        }
        List<DocumentBlock> upgraded = new ArrayList<>();
        for (DocumentBlock block : document.blocks()) {
            upgraded.add(upgrade(block));
        }
        return new ParsedDocument(upgraded);
    }

    /** 至少两个“第X条”，或“第X章”与“第X条”同时出现，才认定是规章制度 */
    private static boolean looksLikeRegulation(List<DocumentBlock> blocks) {
        int clauseCount = 0;
        int chapterCount = 0;
        for (DocumentBlock block : blocks) {
            String text = block.text();
            if (CLAUSE.matcher(text).matches()) {
                clauseCount++;
            } else if (CHAPTER.matcher(text).matches()) {
                chapterCount++;
            }
        }
        return clauseCount >= 2 || (chapterCount >= 1 && clauseCount >= 1);
    }

    private static DocumentBlock upgrade(DocumentBlock block) {
        // 标题与表格保持原样，只升级普通段落和列表行
        if (block.type() != DocumentBlockType.PARAGRAPH && block.type() != DocumentBlockType.LIST_ITEM) {
            return block;
        }
        String text = block.text();
        if (CLAUSE.matcher(text).matches()) {
            return DocumentBlock.clause(text, 2);
        }
        if (CHAPTER.matcher(text).matches()) {
            return DocumentBlock.title(text, 1);
        }
        if (SECTION.matcher(text).matches()) {
            return DocumentBlock.title(text, 2);
        }
        if (SUB_ITEM.matcher(text).matches()) {
            return DocumentBlock.clause(text, 3);
        }
        return block;
    }
}
