package com.gymmind.shared.security;

import java.util.Optional;

public interface CurrentActorProvider {

    Optional<CurrentActor> current();

    CurrentActor requireCurrent();
}
