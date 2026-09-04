package ru.russpass.e2e.base;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import ru.russpass.e2e.config.TestSettings;
import ru.russpass.e2e.helpers.AuthSession;
import ru.russpass.e2e.helpers.BrowserLauncher;
import ru.russpass.e2e.helpers.Env;

import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Timeout(value = 10, unit = TimeUnit.MINUTES)
public abstract class PlaywrightTestBase {

    protected Playwright playwright;
    protected Browser browser;
    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    void launchBrowser() {

        Env.load();

        playwright = Playwright.create();

        browser = BrowserLauncher.launch(playwright);

        AuthSession.ensure(browser);
    }

    @AfterAll
    void closeBrowser() {

        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void createContextAndPage() {

        Browser.NewContextOptions options =
                new Browser.NewContextOptions()
                        .setBaseURL(TestSettings.BASE_URL)
                        .setViewportSize(1280, 720);

        if (Files.exists(TestSettings.AUTH_FILE)) {
            options.setStorageStatePath(
                    TestSettings.AUTH_FILE
            );
        }

        context = browser.newContext(options);

        context.setDefaultTimeout(
                TestSettings.ACTION_TIMEOUT_MS
        );

        context.setDefaultNavigationTimeout(
                TestSettings.NAVIGATION_TIMEOUT_MS
        );

        page = context.newPage();
    }

    @AfterEach
    void closeContext() {

        if (context != null) {
            context.close();
        }
    }
}