package ru.russpass.e2e;

import java.nio.file.Path;

final class TestSettings {
    static final String BASE_URL = "https://portal.dev01.russpass.dev";
    static final Path AUTH_FILE = Path.of("playwright", ".auth", "user.json");
    static final int ACTION_TIMEOUT_MS = 15_000;
    static final int NAVIGATION_TIMEOUT_MS = 60_000;

    private TestSettings() {
    }

    static boolean headed() {
        return Boolean.parseBoolean(firstNonBlank(
                System.getProperty("headed"),
                System.getenv("HEADED"),
                "false"
        ));
    }

    /** chromium (по умолчанию), firefox, webkit. */
    static String browserName() {
        return firstNonBlank(System.getProperty("browser"), System.getenv("BROWSER"), "chromium")
                .toLowerCase();
    }

    /**
     * Канал браузера: chrome, msedge — использует уже установленный Chrome/Edge,
     * без скачивания Chromium с CDN. Пример: -Pchannel=chrome
     */
    static String channel() {
        return firstNonBlank(System.getProperty("channel"), System.getenv("CHANNEL"), "").trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
