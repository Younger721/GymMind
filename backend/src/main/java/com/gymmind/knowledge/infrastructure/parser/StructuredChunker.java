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
