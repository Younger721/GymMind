package com.gymmind.knowledge.infrastructure.parser;

/** Deterministic fallback estimate for environments without the model tokenizer. */
public final class HeuristicTokenCounter implements TokenCounter {
    @Override
    public int count(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        int tokens = 0;
        boolean latinWord = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c) || Character.isWhitespace(c)) {
                latinWord = false;
            } else if (isCjk(c) || isPunctuation(c)) {
                tokens++;
                latinWord = false;
            } else if (!latinWord) {
                tokens++;
                latinWord = true;
            }
        }
        return Math.max(1, tokens);
    }

    private static boolean isCjk(char c) {
        return (c >= '\u3400' && c <= '\u9fff') || (c >= '\uf900' && c <= '\ufaff');
    }

    private static boolean isPunctuation(char c) {
        return "，。！？；：、,.!?;:()[]{}<>《》“”\"'".indexOf(c) >= 0;
    }
}
