package ru.russpass.e2e.helpers;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import ru.russpass.e2e.config.TestSettings;

public final class BrowserLauncher {

    private BrowserLauncher() {
    }

    public static Browser launch(Playwright playwright) {

        String browserName =
                TestSettings.browserName();

        boolean headed =
                TestSettings.headed();

        BrowserType.LaunchOptions options =
                new BrowserType.LaunchOptions()
                        .setHeadless(!headed);

        return switch (browserName.toLowerCase()) {

            case "chromium" ->
                    playwright.chromium()
                            .launch(options);

            case "chrome" ->
                    playwright.chromium()
                            .launch(
                                    options.setChannel("chrome")
                            );

            case "edge" ->
                    playwright.chromium()
                            .launch(
                                    options.setChannel("msedge")
                            );

            case "firefox" ->
                    playwright.firefox()
                            .launch(options);

            case "webkit" ->
                    playwright.webkit()
                            .launch(options);

            default ->
                    throw new IllegalArgumentException(
                            "Неизвестный браузер: "
                                    + browserName
                                    + ". Используй: chromium, chrome, edge, firefox или webkit."
                    );
        };
    }
}
