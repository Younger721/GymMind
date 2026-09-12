package com.gymmind.shared.security.jwt;

import com.gymmind.shared.config.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.util.UUID;

@Configuration(proxyBeanMethods = false)
public class JwtConfiguration {

    @Bean
    Clock jwtClock() {
        return Clock.systemUTC();
    }

    @Bean
    TokenIdGenerator tokenIdGenerator() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
    JwtService jwtService(
            SecurityProperties securityProperties,
            Clock jwtClock,
            TokenIdGenerator tokenIdGenerator) {
        return new JjwtJwtService(securityProperties, jwtClock, tokenIdGenerator);
    }
}
