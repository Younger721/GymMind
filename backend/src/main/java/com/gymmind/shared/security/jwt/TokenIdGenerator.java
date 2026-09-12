package com.gymmind.shared.security.jwt;

@FunctionalInterface
public interface TokenIdGenerator {

    String generate();
}
