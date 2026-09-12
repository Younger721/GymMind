package com.gymmind.shared.security;

import java.security.Principal;
import java.util.Objects;

public record GymMindPrincipal(CurrentActor actor) implements Principal {

    public GymMindPrincipal {
        Objects.requireNonNull(actor, "actor");
    }

    @Override
    public String getName() {
        return actor.userId().toString();
    }

    public String getCredentials() {
        return "";
    }

    @Override
    public String toString() {
        return "GymMindPrincipal[userId=" + actor.userId()
                + ", tenantId=" + actor.tenantId() + "]";
    }
}
