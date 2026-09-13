package com.gymmind.platform.database;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseNameGuardTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"GymMind", "mysql", "information_schema", "gymmind ",
            "gymmind;DROP DATABASE mysql"})
    void rejectsEveryDatabaseExceptExactGymmind(String name) {
        assertThatThrownBy(() -> DatabaseNameGuard.requireAllowed(name))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
