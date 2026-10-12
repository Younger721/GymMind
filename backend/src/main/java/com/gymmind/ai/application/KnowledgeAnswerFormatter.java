package com.gymmind.ai.application;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将知识库问答正文格式化为「根据《文档》…条例可得，…」的引用句式。
 */
public final class KnowledgeAnswerFormatter {

    private static final Pattern CLAUSE_IN_BRACKETS = Pattern.compile("【([^】]{2,64})】");
    private static final Pattern PLAT_OPS = Pattern.compile("(PLAT-OPS-\\d+)");
    private static final Pattern EVAL_SAFE = Pattern.compile("(EVAL-PLAT-SAFE-\\d+)");

    private KnowledgeAnswerFormatter() {
    }

    public static String apply(String answer, List<SourceCitation> citations) {
        if (answer == null || answer.isBlank() || citations == null || citations.isEmpty()) {
            return answer;
        }
        String trimmed = answer.trim();
        if (trimmed.startsWith("根据")) {
            return trimmed;
        }
        SourceCitation primary = citations.get(0);
        String docTitle = displayDocumentTitle(primary.documentName());
        String clause = resolveClauseLabel(primary.excerpt());
        String prefix = clause == null
                ? "根据《" + docTitle + "》相关条款可得，"
                : "根据《" + docTitle + "》" + clause + "可得，";
        return prefix + trimmed;
    }

    private static String displayDocumentTitle(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "知识库文档";
        }
        String name = fileName.trim();
        if (name.toLowerCase().endsWith(".docx")) {
            return name.substring(0, name.length() - 5);
        }
        if (name.toLowerCase().endsWith(".md")) {
            return name.substring(0, name.length() - 3);
        }
        return name;
    }

    private static String resolveClauseLabel(String excerpt) {
        if (excerpt == null || excerpt.isBlank()) {
            return null;
        }
        Matcher bracket = CLAUSE_IN_BRACKETS.matcher(excerpt);
        if (bracket.find()) {
            return bracket.group(1).trim();
        }
        Matcher plat = PLAT_OPS.matcher(excerpt);
        if (plat.find()) {
            return plat.group(1);
        }
        Matcher eval = EVAL_SAFE.matcher(excerpt);
        if (eval.find()) {
            return eval.group(1);
        }
        return null;
    }
}
