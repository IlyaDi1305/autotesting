package ru.russpass.e2e;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.Assumptions;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

final class AuthSession {
    private static final Pattern LOGIN_FIELD = Pattern.compile(
            "Телефон|электронная почта|СНИЛС",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SMS_TEXT = Pattern.compile(
            "код подтверждения из SMS",
            Pattern.CASE_INSENSITIVE
    );

    private AuthSession() {
    }

    static void ensure(Browser browser) {
        String login = Env.get("RUSSPASS_LOGIN");
        String password = Env.get("RUSSPASS_PASSWORD");
        Assumptions.assumeFalse(
                login.isBlank() || password.isBlank(),
                "Заполните RUSSPASS_LOGIN и RUSSPASS_PASSWORD в .env"
        );

        try {
            Files.createDirectories(TestSettings.AUTH_FILE.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        // Если сохранённая сессия ещё жива — не логинимся заново (и не ловим SMS).
        if (Files.exists(TestSettings.AUTH_FILE) && isSessionAlive(browser)) {
            return;
        }

        loginViaMosRu(browser, login, password);
    }

    private static boolean isSessionAlive(Browser browser) {
        try (BrowserContext context = newContext(browser, true)) {
            Page page = context.newPage();
            page.navigate("/");
            PageActions.acceptCookies(page);
            return page.locator("a[href=\"/loyalty\"]").count() > 0
                    && page.locator("a[href=\"/loyalty\"]").first().isVisible();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static void loginViaMosRu(Browser browser, String login, String password) {
        try (BrowserContext context = newContext(browser, false)) {
            Page page = context.newPage();
            page.navigate("/");
            PageActions.acceptCookies(page);

            String redirect = URLEncoder.encode(TestSettings.BASE_URL + "/", StandardCharsets.UTF_8);
            page.navigate("/id?source=russpass&redirect-url=" + redirect);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Mos.ru")).click();
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(LOGIN_FIELD)).fill(login);
            page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Пароль")).fill(password);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Войти")).last().click();

            // Иногда Mos.ru просит SMS. Введите код в окне браузера вручную.
            try {
                page.getByText(SMS_TEXT).waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15_000));
                System.out.println();
                System.out.println(">>> Введите SMS-код в окне браузера и нажмите «Подтвердить» (ждём до 5 минут)...");
                System.out.println();
            } catch (PlaywrightException ignored) {
                // SMS-экран не появился
            }

            waitForPortalOutsideId(page, 300_000);

            assertThat(page.locator("a[href=\"/loyalty\"]"))
                    .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));

            context.storageState(new BrowserContext.StorageStateOptions().setPath(TestSettings.AUTH_FILE));
        }
    }

    /**
     * Аналог TS-предиката в page.waitForURL: ждём возврата на портал вне раздела /id.
     * Опрашиваем URL из Java, т.к. при переходах между mos.ru и порталом контекст страницы пересоздаётся.
     */
    static void waitForPortalOutsideId(Page page, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (isPortalOutsideId(page.url())) {
                return;
            }
            page.waitForTimeout(500);
        }
        throw new AssertionError("Не дождались возврата на портал вне /id, текущий URL: " + page.url());
    }

    static boolean isPortalOutsideId(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            String host = uri.getHost();
            String path = uri.getPath() == null ? "" : uri.getPath();
            return host != null
                    && host.contains("portal.dev01.russpass.dev")
                    && !path.contains("/id");
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static BrowserContext newContext(Browser browser, boolean withStorage) {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setBaseURL(TestSettings.BASE_URL)
                .setViewportSize(1280, 720);
        if (withStorage) {
            options.setStorageStatePath(TestSettings.AUTH_FILE);
        }
        BrowserContext context = browser.newContext(options);
        context.setDefaultTimeout(TestSettings.ACTION_TIMEOUT_MS);
        context.setDefaultNavigationTimeout(TestSettings.NAVIGATION_TIMEOUT_MS);
        return context;
    }
}
