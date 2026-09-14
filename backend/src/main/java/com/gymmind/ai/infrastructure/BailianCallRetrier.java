package com.gymmind.ai.infrastructure;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.function.Consumer;

final class BailianCallRetrier {
    private final int maxAttempts;
    private final Consumer<Long> sleeper;

    BailianCallRetrier(int maxAttempts, Consumer<Long> sleeper) {
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be positive");
        this.maxAttempts = maxAttempts;
        this.sleeper = sleeper;
    }

    <T> T execute(java.util.function.Supplier<T> call) {
        RuntimeException last = null;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.get();
            } catch (RuntimeException failure) {
                last = failure;
                if (attempt == maxAttempts || !retryable(failure)) throw failure;
                sleeper.accept(100L * (1L << (attempt - 1)));
            }
        }
        throw last;
    }

    private static boolean retryable(RuntimeException failure) {
        return failure instanceof ResourceAccessException
                || failure instanceof HttpServerErrorException
                || failure instanceof HttpClientErrorException client && client.getStatusCode().value() == 429;
    }
}
