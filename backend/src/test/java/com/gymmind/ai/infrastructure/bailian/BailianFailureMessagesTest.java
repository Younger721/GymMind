package com.gymmind.ai.infrastructure.bailian;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpClientErrorException;

import static org.assertj.core.api.Assertions.assertThat;

class BailianFailureMessagesTest {

    @Test
    void mapsInvalidApiKeyFromUnauthorized() {
        var ex = HttpClientErrorException.Unauthorized.create(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                org.springframework.http.HttpHeaders.EMPTY,
                "{\"code\":\"invalid_api_key\"}".getBytes(),
                null);
        assertThat(BailianFailureMessages.dependencyUnavailableMessage(ex))
                .contains("无效或已失效");
    }
}
