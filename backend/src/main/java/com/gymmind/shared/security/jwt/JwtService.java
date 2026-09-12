package com.gymmind.shared.security.jwt;

import com.gymmind.shared.security.CurrentActor;

public interface JwtService {

    TokenPair issue(CurrentActor actor);

    JwtClaims verify(String rawToken, TokenType expectedType);
}
