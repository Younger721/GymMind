package com.gymmind.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ChunkingService {

    @Value("${rag.chunk.size:512}")
    private int chunkSize;

    @Value("${rag.chunk.overlap:50}")
    private int chunkOverlap;

    public List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // Simple sliding window chunking
        int start = 0;
        int textLength = text.length();

        while (start < textLength) {
            int end = Math.min(start + chunkSize, textLength);

            // Try to break at sentence or word boundary
            if (end < textLength) {
                // Look for sentence end
                int sentenceEnd = findSentenceEnd(text, start, end);
                if (sentenceEnd > start) {
                    end = sentenceEnd;
                } else {
                    // Look for word boundary
                    int wordEnd = findWordBoundary(text, end);
                    if (wordEnd > start) {
                        end = wordEnd;
                    }
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            // Move to next chunk with overlap
            start = end - chunkOverlap;
            if (start < 0) start = 0;

            // Prevent infinite loop
            if (start == end) {
                start = end;
            }
        }

        log.debug("Split text into {} chunks (size: {}, overlap: {})",
                chunks.size(), chunkSize, chunkOverlap);

        return chunks;
    }

    private int findSentenceEnd(String text, int start, int end) {
        String substring = text.substring(start, end);
        int lastPeriod = substring.lastIndexOf('。');
        if (lastPeriod == -1) {
            lastPeriod = substring.lastIndexOf('.');
        }
        if (lastPeriod == -1) {
            lastPeriod = substring.lastIndexOf('！');
        }
        if (lastPeriod == -1) {
            lastPeriod = substring.lastIndexOf('!');
        }
        if (lastPeriod == -1) {
            lastPeriod = substring.lastIndexOf('？');
        }
        if (lastPeriod == -1) {
            lastPeriod = substring.lastIndexOf('?');
        }

        return lastPeriod > 0 ? start + lastPeriod + 1 : -1;
    }

    private int findWordBoundary(String text, int position) {
        if (position >= text.length()) {
            return text.length();
        }

        // Look backward for space
        for (int i = position; i > position - 50 && i >= 0; i--) {
            if (Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }

        return position;
    }
}
