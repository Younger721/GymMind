package com.gymmind.platform.bootstrap;

import com.gymmind.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseInitializationLockIT extends MySqlIntegrationTest {

    @Autowired private DatabaseInitializationLock initializationLock;

    @Test
    void serializesInitializationAcrossDatabaseConnections() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        try {
            Future<?> first = executor.submit(() -> initializationLock.execute("gymmind:test:init", () -> {
                firstEntered.countDown();
                await(releaseFirst);
            }));
            assertThat(firstEntered.await(5, TimeUnit.SECONDS)).isTrue();

            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                initializationLock.execute("gymmind:test:init", secondEntered::countDown);
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(secondEntered.await(300, TimeUnit.MILLISECONDS)).isFalse();

            releaseFirst.countDown();
            first.get();
            second.get();
            assertThat(secondEntered.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test coordination", exception);
        }
    }
}
