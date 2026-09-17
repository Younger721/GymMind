package com.gymmind.support;

/**
 * Marker for optional Docker-backed integration tests.
 * Run with {@code mvn verify -Pintegration} when containers are available.
 */
public final class IntegrationTestSupport {

    private IntegrationTestSupport() {
    }
}
