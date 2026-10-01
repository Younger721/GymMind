package com.gymmind.knowledge.infrastructure.parser;

/** Counts model input tokens without tying chunking to a specific tokenizer library. */
@FunctionalInterface
public interface TokenCounter {
    int count(String text);
}
