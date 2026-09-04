package ru.russpass.e2e;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Env {
    private static final Map<String, String> FILE_VALUES = new LinkedHashMap<>();
    private static boolean loaded;

    private Env() {
    }

    public static synchronized void load() {
        if (loaded) {
            return;
        }
        Path envFile = findEnvFile();
        if (envFile != null) {
            try {
                for (String rawLine : Files.readAllLines(envFile, StandardCharsets.UTF_8)) {
                    String line = rawLine.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eq = line.indexOf('=');
                    if (eq <= 0) {
                        continue;
                    }
                    String key = line.substring(0, eq).trim();
                    String value = stripQuotes(line.substring(eq + 1).trim());
                    FILE_VALUES.put(key, value);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Не удалось прочитать " + envFile, e);
            }
        }
        loaded = true;
    }

    public static String get(String key) {
        load();
        String fromEnv = System.getenv(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv;
        }
        String fromFile = FILE_VALUES.get(key);
        return fromFile == null ? "" : fromFile;
    }

    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value.isBlank() ? defaultValue : value;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }

    private static Path findEnvFile() {
        Path dir = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int i = 0; i < 5 && dir != null; i++) {
            Path candidate = dir.resolve(".env");
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            dir = dir.getParent();
        }
        return null;
    }
}
