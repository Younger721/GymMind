package com.gymmind.knowledge.infrastructure.parser;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/** Chunks parsed blocks while preserving the boundaries important to fitness content. */
public final class StructuredChunker {
    private static final int MAX_TOKENS = 600;
    private static final int MAX_CHARS = 1200;
    private static final int KNOWLEDGE_OVERLAP_TOKENS = 70;
    private final TokenCounter tokenCounter;

    public StructuredChunker(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter == null ? new HeuristicTokenCounter() : tokenCounter;
    }

    public List<Chunk> chunk(ParsedDocument document, String documentId, ChunkStrategy strategy) {
        return chunk(document, documentId, strategy, 1);
    }

    public List<Chunk> chunk(ParsedDocument document, String documentId, ChunkStrategy strategy, int strategyVersion) {
        if (document == null || document.blocks().isEmpty() || documentId == null || documentId.isBlank()) {
            throw new IllegalArgumentException("empty document");
        }
        if (strategyVersion <= 0) {
            throw new IllegalArgumentException("strategy version must be positive");
        }
        ChunkStrategy selected = strategy == null ? ChunkStrategy.GENERAL : strategy;
        List<String> pieces = switch (selected) {
            case PROCEDURE -> procedure(document.blocks());
            case FAQ_RULE -> faq(document.blocks());
            case RECORD -> records(document.blocks());
            case KNOWLEDGE -> knowledge(document.blocks());
            case REGULATION -> regulation(document.blocks());
            case GENERAL -> general(document.blocks());
        };
        List<Chunk> result = new ArrayList<>();
        for (int i = 0; i < pieces.size(); i++) {
            String text = pieces.get(i).trim();
            if (!text.isBlank()) result.add(new Chunk(hash(documentId + ":" + selected + ":v" + strategyVersion + ":" + i + ":" + text), documentId, i, text));
        }
        if (result.isEmpty()) throw new IllegalArgumentException("empty document");
        return result;
    }

    private List<String> procedure(List<DocumentBlock> blocks) {
        return grouped(blocks, false, true);
    }

    /**
     * 规章制度切片：一条 = 一个片段，款项并入所属条，超长条按句子边界降级切分。
     *
     * <p>每个片段头部带「【章节 · 条号】」面包屑，既补足条款单独成块后缺失的主题语境，
     * 也与 {@code KnowledgeAnswerFormatter} 的「根据《文档》…可得」引用句式对齐。</p>
     */
    private List<String> regulation(List<DocumentBlock> blocks) {
        boolean hasClauses = blocks.stream().anyMatch(block -> block.type() == DocumentBlockType.CLAUSE);
        if (!hasClauses) {
            // 没有可识别的条款编号时退回标题边界聚合，保证兜底行为与 GENERAL 一致
            return grouped(blocks, true, false);
        }
        List<String> pieces = new ArrayList<>();
        StringBuilder clauseBuffer = new StringBuilder();
        String clauseLabel = null;
        String section = null;
        for (DocumentBlock block : blocks) {
            switch (block.type()) {
                case TITLE -> {
                    // 章/节标题：刷新当前条款，标题本身只作上下文，不单独成块
                    flushRegulationClause(pieces, clauseBuffer, clauseLabel, section);
                    clauseBuffer.setLength(0);
                    clauseLabel = null;
                    section = block.text();
                }
                case CLAUSE -> {
                    if (block.level() <= 2) {
                        // “条”是新的切片边界
                        flushRegulationClause(pieces, clauseBuffer, clauseLabel, section);
                        clauseBuffer.setLength(0);
                        clauseLabel = clauseLabel(block.text());
                    } else {
                        // “款/项”并入当前条；若条尚未开始，则以章节名作为标签先起一块
                        if (clauseBuffer.length() == 0 && clauseLabel == null) {
                            clauseLabel = section;
                        }
                    }
                    append(clauseBuffer, block.text());
                }
                default -> {
                    // 普通段落/列表/表格行：条款前导内容（如总则、目的）以章节名作标签
                    if (clauseBuffer.length() == 0 && clauseLabel == null) {
                        clauseLabel = section;
                    }
                    append(clauseBuffer, block.text());
                }
            }
        }
        flushRegulationClause(pieces, clauseBuffer, clauseLabel, section);
        return pieces;
    }

    /** 条款标签取条号开头的第一句（去句末标点、截断到 40 字），如「第十二条 会员退卡时应凭有效证件」 */
    private static String clauseLabel(String text) {
        String firstLine = text.split("\\R", 2)[0].trim();
        int sentenceEnd = indexOfSentenceEnd(firstLine);
        if (sentenceEnd > 0) {
            firstLine = firstLine.substring(0, sentenceEnd);
        }
        return firstLine.length() > 40 ? firstLine.substring(0, 40) : firstLine;
    }

    private static int indexOfSentenceEnd(String text) {
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            if (current == '。' || current == '！' || current == '？' || current == '；') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 条款缓冲落盘：面包屑 + 正文；超过单块上限时按句子边界切分，且每片都带同一面包屑。
     *
     * @param label   条款标签（条号或章节名）；标签本身就是当前章节名时不再重复拼接
     * @param section 当前所属章节标题，拼成「章节 · 条款」形式的面包屑
     */
    private static void flushRegulationClause(List<String> pieces, StringBuilder buffer, String label, String section) {
        String body = buffer.toString().trim();
        if (body.isBlank()) {
            return;
        }
        // 有面包屑时给正文留出前缀空间，避免切片略超上限
        int limit = label == null ? MAX_CHARS : MAX_CHARS - 80;
        List<String> parts = splitBySentence(body, limit);
        if (label == null || label.isBlank()) {
            pieces.addAll(parts);
            return;
        }
        String breadcrumbLabel = label;
        boolean sectionIsContext = section != null && !section.isBlank() && !section.equals(label);
        if (sectionIsContext) {
            breadcrumbLabel = section + " · " + label;
        }
        String breadcrumb = "【" + breadcrumbLabel + "】";
        for (String part : parts) {
            pieces.add(breadcrumb + "\n" + part);
        }
    }

    /** 按句子边界聚合到 limit 上限；单句超长时退回固定字符窗口，保证不丢内容 */
    private static List<String> splitBySentence(String text, int limit) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String sentence : text.split("(?<=。|！|？|；|\\n)")) {
            if (sentence.isBlank()) {
                continue;
            }
            if (sentence.length() > limit) {
                if (current.length() > 0) {
                    parts.add(current.toString());
                    current.setLength(0);
                }
                parts.addAll(splitBounded(sentence));
                continue;
            }
            if (current.length() + sentence.length() > limit && current.length() > 0) {
                parts.add(current.toString());
                current.setLength(0);
            }
            current.append(sentence);
        }
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        return parts;
    }

    private List<String> knowledge(List<DocumentBlock> blocks) {
        List<String> result = grouped(blocks, true, false);
        if (result.size() <= 1 || KNOWLEDGE_OVERLAP_TOKENS <= 0) return result;
        List<String> overlapped = new ArrayList<>();
        for (int i = 0; i < result.size(); i++) {
            String current = result.get(i);
            if (i > 0) {
                String prior = result.get(i - 1);
                String[] words = prior.split("(?<=\\s)|(?=\\s)");
                int take = Math.min(words.length, KNOWLEDGE_OVERLAP_TOKENS);
                StringBuilder tail = new StringBuilder();
                for (int j = words.length - take; j < words.length; j++) tail.append(words[j]);
                String prefix = tail.toString();
                int available = Math.max(0, MAX_CHARS - current.length() - 1);
                if (prefix.length() > available) prefix = prefix.substring(Math.max(0, prefix.length() - available));
                current = prefix.isBlank() ? current : prefix + "\n" + current;
            }
            overlapped.add(current);
        }
        return overlapped;
    }

    private List<String> faq(List<DocumentBlock> blocks) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (DocumentBlock block : blocks) {
            String text = block.text();
            boolean question = isQuestion(text) || block.type() == DocumentBlockType.TITLE;
            if (question && current.length() > 0) {
                result.add(current.toString());
                current.setLength(0);
            }
            append(current, text);
            if (current.length() >= MAX_CHARS) {
                result.addAll(splitBounded(current.toString()));
                current.setLength(0);
            }
        }
        if (current.length() > 0) result.addAll(splitBounded(current.toString()));
        return result;
    }

    private List<String> records(List<DocumentBlock> blocks) {
        List<String> result = new ArrayList<>();
        for (DocumentBlock block : blocks) {
            if (block.type() == DocumentBlockType.TABLE_ROW && block.hasTableHeaders()) {
                String headers = String.join(" | ", block.tableHeaders());
                result.addAll(splitBounded(block.text().startsWith(headers)
                        ? block.text() : headers + "\n" + block.text()));
            } else {
                result.addAll(splitBounded(block.text()));
            }
        }
        return result;
    }

    private List<String> general(List<DocumentBlock> blocks) {
        // Even the fallback strategy keeps parser boundaries. A fixed character
        // window can combine unrelated sections and makes every citation look
        // like a copy of the whole document.
        return grouped(blocks, true, false);
    }

    private List<String> grouped(List<DocumentBlock> blocks, boolean splitOnTitle, boolean keepListGroup) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (DocumentBlock block : blocks) {
            if (block.text().length() > MAX_CHARS) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current.setLength(0);
                }
                result.addAll(splitBounded(block.text()));
                continue;
            }
            boolean boundary = current.length() > 0 && ((splitOnTitle && block.type() == DocumentBlockType.TITLE)
                    || (!keepListGroup && block.type() == DocumentBlockType.TITLE && tokenCounter.count(current + "\n" + block.text()) > MAX_TOKENS)
                    || tokenCounter.count(current + "\n" + block.text()) > MAX_TOKENS
                    || current.length() + block.text().length() > MAX_CHARS);
            if (boundary) {
                result.add(current.toString());
                current.setLength(0);
            }
            append(current, block.text());
        }
        if (current.length() > 0) result.addAll(splitBounded(current.toString()));
        return result;
    }

    private static List<String> splitBounded(String text) {
        List<String> pieces = new ArrayList<>();
        if (text == null || text.isBlank()) return pieces;
        for (int start = 0; start < text.length(); start += MAX_CHARS) {
            int end = Math.min(text.length(), start + MAX_CHARS);
            pieces.add(text.substring(start, end));
            if (end == text.length()) break;
        }
        return pieces;
    }

    private static void append(StringBuilder target, String text) {
        if (target.length() > 0) target.append('\n');
        target.append(text);
    }

    private static boolean isQuestion(String text) {
        return text.contains("?") || text.contains("？") || text.matches("(?i)^(问|q)\\s*[:：].*");
    }

    private static String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte valueByte : bytes) result.append(String.format("%02x", valueByte));
            return result.toString();
        } catch (Exception e) {
            throw new IllegalStateException("unable to hash chunk", e);
        }
    }
}
