package com.gymmind.knowledge.infrastructure.parser;

import com.gymmind.ai.application.ChatModelGateway;
import com.gymmind.ai.domain.ContextSegment;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Deterministic first-pass strategy selection used before an optional LLM advisor. */
public final class ChunkStrategySelector {
    private static final Pattern STRATEGY = Pattern.compile("\\\"strategy\\\"\\s*:\\s*\\\"([A-Z_]+)\\\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern CONFIDENCE = Pattern.compile("\\\"confidence\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private final ChatModelGateway advisor;

    public ChunkStrategySelector() {
        this(null);
    }

    public ChunkStrategySelector(ChatModelGateway advisor) {
        this.advisor = advisor;
    }

    public ChunkStrategy select(String category, String fileName, String preview) {
        ChunkStrategy rule = selectByRules(category, fileName, preview);
        if (rule != ChunkStrategy.GENERAL || advisor == null) {
            return rule;
        }
        try {
            String prompt = "将健身知识文档归类为 PROCEDURE、KNOWLEDGE、FAQ_RULE、RECORD、GENERAL 之一。"
                    + "只返回 JSON：{\\\"strategy\\\":\\\"ENUM\\\",\\\"confidence\\\":0到1}。"
                    + "资料分类=" + safe(category) + "，文件名=" + safe(fileName) + "，正文前文=" + safe(preview);
            String response = advisor.complete(prompt, List.<ContextSegment>of());
            Matcher strategyMatcher = STRATEGY.matcher(response == null ? "" : response);
            Matcher confidenceMatcher = CONFIDENCE.matcher(response == null ? "" : response);
            if (!strategyMatcher.find() || !confidenceMatcher.find()) return rule;
            double confidence = Double.parseDouble(confidenceMatcher.group(1));
            if (confidence < 0.7d || confidence > 1.0d) return rule;
            return ChunkStrategy.valueOf(strategyMatcher.group(1).toUpperCase(Locale.ROOT));
        } catch (RuntimeException ignored) {
            return rule;
        }
    }

    private ChunkStrategy selectByRules(String category, String fileName, String preview) {
        String c = normalize(category);
        String name = normalize(fileName);
        String content = normalize(preview);
        if (containsAny(c, "FAQ", "FAQ_RULE", "问答", "规则", "制度", "安全", "GYM_RULES")
                || containsAny(name, "faq", "rule", "policy", "制度", "安全")
                || content.matches("(?s).*(问[:：].*答[:：]|问题[:：].*答案[:：]).*")) {
            return ChunkStrategy.FAQ_RULE;
        }
        if (containsAny(c, "课程", "COURSE", "套餐", "排课", "记录", "时间表")
                || containsAny(name, "course", "schedule", "package", "套餐", "课程", "排课")
                || looksLikeTable(content)) {
            return ChunkStrategy.RECORD;
        }
        if (containsAny(c, "动作", "ACTION_STANDARD", "训练", "器械", "EQUIPMENT", "步骤", "操作")
                || containsAny(name, "exercise", "workout", "squat", "动作", "训练", "器械", "步骤")
                || content.matches("(?s).*(步骤\\s*[一二三四五六七八九十0-9]|step\\s*[0-9]).*")) {
            return ChunkStrategy.PROCEDURE;
        }
        if (containsAny(c, "知识", "营养", "NUTRITION", "说明", "教程", "基础", "TRAINING_BASICS")
                || content.length() > 3000) {
            return ChunkStrategy.KNOWLEDGE;
        }
        return ChunkStrategy.GENERAL;
    }

    private static String safe(String value) {
        if (value == null) return "";
        return value.length() > 4000 ? value.substring(0, 4000) : value;
    }

    private static boolean looksLikeTable(String content) {
        String[] lines = content.split("\\R");
        if (lines.length < 2) return false;
        long rows = java.util.Arrays.stream(lines).filter(line -> line.contains("|") || line.split("[,，\\t]").length >= 3).count();
        return rows >= 2;
    }

    private static boolean containsAny(String value, String... terms) {
        for (String term : terms) if (value.contains(normalize(term))) return true;
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
