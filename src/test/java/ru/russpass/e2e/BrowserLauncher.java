package ru.russpass.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.nio.file.Files;
import java.nio.file.Path;

final class BrowserLauncher {
    private BrowserLauncher() {
    }

    static Browser launch(Playwright playwright) {
        BrowserType browserType = switch (TestSettings.browserName()) {
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> playwright.chromium();
        };

        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions()
                .setHeadless(!TestSettings.headed());

        String channel = resolveChannel();
        if (channel != null) {
            options.setChannel(channel);
            System.out.println(">>> Браузер: " + channel);
        } else {
            System.out.println(">>> Браузер: Playwright Chromium (из %LOCALAPPDATA%\\ms-playwright)");
        }

        return browserType.launch(options);
    }

    /**
     * -Dchannel=chrome|msedge — явный выбор.
     * Если указанный браузер не найден — fallback на скачанный Playwright Chromium.
     */
    private static String resolveChannel() {
        String requested = TestSettings.channel();
        if (requested.isEmpty()) {
            return null;
        }

        Path executable = channelExecutable(requested);
        if (executable != null && Files.isRegularFile(executable)) {
            return requested;
        }

        System.out.println();
        System.out.println(">>> Внимание: браузер '" + requested + "' не найден по пути "
                + (executable == null ? "(неизвестный channel)" : executable));
        System.out.println(">>> Используем Playwright Chromium из ms-playwright.");
        System.out.println(">>> Чтобы не скачивать Chromium, установите Google Chrome или используйте -Dchannel=msedge");
        System.out.println();
        return null;
    }

    private static Path channelExecutable(String channel) {
        String localAppData = System.getenv("LOCALAPPDATA");
        if (localAppData == null) {
            return null;
        }
        return switch (channel.toLowerCase()) {
            case "chrome" -> Path.of(localAppData, "Google", "Chrome", "Application", "chrome.exe");
            case "msedge" -> Path.of(localAppData, "Microsoft", "Edge", "Application", "msedge.exe");
            default -> null;
        };
    }
}
