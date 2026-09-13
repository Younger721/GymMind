package com.gymmind.platform.database;

public final class DatabaseNameGuard {

    public static final String ALLOWED_DATABASE = "gymmind";

    private DatabaseNameGuard() {
    }

    public static String requireAllowed(String databaseName) {
        if (!ALLOWED_DATABASE.equals(databaseName)) {
            throw new IllegalArgumentException("Only the exact gymmind database may be reset");
        }
        return databaseName;
    }
}
