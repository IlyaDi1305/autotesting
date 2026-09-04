package ru.russpass.e2e.config;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class TestSettings {

    private TestSettings() {
    }

    public static final String BASE_URL =
            System.getenv().getOrDefault(
                    "RUSSPASS_BASE_URL",
                    "https://portal.dev01.russpass.dev"
            );

    public static final Path AUTH_FILE =
            Paths.get("playwright", ".auth", "user.json");

    public static final int ACTION_TIMEOUT_MS = 15_000;

    public static final int NAVIGATION_TIMEOUT_MS = 30_000;

    public static String browserName() {
        return System.getenv().getOrDefault(
                "PLAYWRIGHT_BROWSER",
                "chromium"
        );
    }

    public static boolean headed() {
        return Boolean.parseBoolean(
                System.getenv().getOrDefault(
                        "PLAYWRIGHT_HEADED",
                        "true"
                )
        );
    }
}