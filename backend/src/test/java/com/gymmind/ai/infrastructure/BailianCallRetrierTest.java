package com.gymmind.ai.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

class BailianCallRetrierTest {
    @Test
    void retriesTimeoutRateLimitAndServerErrors() {
        assertThat(attemptsBeforeSuccess(new ResourceAccessException("timeout"))).isEqualTo(2);
        assertThat(attemptsBeforeSuccess(HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS,
                "limited", HttpHeaders.EMPTY, null, null))).isEqualTo(2);
        assertThat(attemptsBeforeSuccess(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE))).isEqualTo(2);
    }

    @Test
    void doesNotRetryOtherClientErrors() {
        AtomicInteger attempts = new AtomicInteger();
        BailianCallRetrier retrier = new BailianCallRetrier(3, ignored -> { });

        assertThatThrownBy(() -> retrier.execute(() -> {
            attempts.incrementAndGet();
            throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
        })).isInstanceOf(HttpClientErrorException.class);
        assertThat(attempts).hasValue(1);
    }

    private int attemptsBeforeSuccess(RuntimeException firstFailure) {
        AtomicInteger attempts = new AtomicInteger();
        BailianCallRetrier retrier = new BailianCallRetrier(3, ignored -> { });
        assertThat(retrier.execute(() -> attempts.incrementAndGet() == 1 ? fail(firstFailure) : "ok"))
                .isEqualTo("ok");
        return attempts.get();
    }

    private static String fail(RuntimeException failure) {
        throw failure;
    }
}
