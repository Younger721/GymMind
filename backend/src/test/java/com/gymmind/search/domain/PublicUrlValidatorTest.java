package com.gymmind.search.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicUrlValidatorTest {
    private final PublicUrlValidator validator = new PublicUrlValidator();

    @Test
    void acceptsHttpAndHttpsPublicHostUrls() {
        assertThat(validator.isAllowed("https://example.com/article")).isTrue();
        assertThat(validator.isAllowed("http://8.8.8.8/news")).isTrue();
    }

    @Test
    void rejectsUnsupportedSchemesAndCredentials() {
        assertThat(validator.isAllowed("file:///etc/passwd")).isFalse();
        assertThat(validator.isAllowed("https://user:pass@example.com/a")).isFalse();
    }

    @Test
    void rejectsLoopbackAndPrivateNetworkTargets() {
        assertThat(validator.isAllowed("http://localhost/admin")).isFalse();
        assertThat(validator.isAllowed("http://127.0.0.1/admin")).isFalse();
        assertThat(validator.isAllowed("http://192.168.1.10/admin")).isFalse();
        assertThat(validator.isAllowed("http://169.254.1.1/metadata")).isFalse();
    }
}
