package ru.russpass.e2e.helpers;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.Cookie;
import org.junit.jupiter.api.Assumptions;
import ru.russpass.e2e.config.TestSettings;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class AuthSession {
    private static final Pattern LOGIN_FIELD = Pattern.compile(
            "Телефон|электронная почта|СНИЛС|Логин",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern SMS_TEXT = Pattern.compile(
            "код подтверждения из SMS",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern TOKEN_EXPIRES = Pattern.compile("\"expires\"\\s*:\\s*\"([^\"]+)\"");

    private AuthSession() {
    }

    public static void ensure(Browser browser) {
        String login = Env.get("RUSSPASS_LOGIN");
        String password = Env.get("RUSSPASS_PASSWORD");
        if (login.isBlank() || password.isBlank()) {
            System.out.println(">>> Нет RUSSPASS_LOGIN / RUSSPASS_PASSWORD в .env — авторизация пропущена");
        }
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
            System.out.println(">>> Авторизация: берём сохранённую сессию " + TestSettings.AUTH_FILE);
            return;
        }
        if (Files.exists(TestSettings.AUTH_FILE)) {
            System.out.println(">>> Авторизация: сохранённая сессия протухла, вход через Mos.ru");
        } else {
            System.out.println(">>> Авторизация: вход через Mos.ru (если будет SMS — введите код в окне браузера)");
        }

        loginViaMosRu(browser, login, password);
    }

    private static boolean isSessionAlive(Browser browser) {
        try (BrowserContext context = newContext(browser, true)) {
            if (!hasFreshAccessToken(context)) {
                System.out.println(">>> Авторизация: access-token в user.json просрочен");
                return false;
            }
            Page page = context.newPage();
            page.navigate("/");
            PageActions.acceptCookies(page);
            page.waitForTimeout(2_000);
            if (!hasFreshAccessToken(context)) {
                System.out.println(">>> Авторизация: токен не обновился после открытия портала");
                return false;
            }
            return page.locator("a[href=\"/loyalty\"]").count() > 0
                    && page.locator("a[href=\"/loyalty\"]").first().isVisible();
        } catch (RuntimeException e) {
            System.out.println(">>> Авторизация: проверка сессии не удалась: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cookie authorized-new живёт ~30 дней, JWT access-token — около 2 часов.
     * Ссылка /loyalty из-за этого может быть видна у уже разлогиненной сессии.
     */
    private static boolean hasFreshAccessToken(BrowserContext context) {
        for (Cookie cookie : context.cookies()) {
            if (!"auth-token-new".equals(cookie.name) || cookie.value == null || cookie.value.isBlank()) {
                continue;
            }
            Matcher matcher = TOKEN_EXPIRES.matcher(cookie.value);
            if (!matcher.find()) {
                continue;
            }
            try {
                Instant expires = Instant.parse(matcher.group(1));
                return expires.isAfter(Instant.now().plusSeconds(60));
            } catch (RuntimeException ignored) {
                return false;
            }
        }
        return false;
    }

    private static void loginViaMosRu(Browser browser, String login, String password) {
        try (BrowserContext context = newContext(browser, false)) {
            Page page = context.newPage();
            page.navigate("/");
            PageActions.acceptCookies(page);

            String redirect = URLEncoder.encode(TestSettings.BASE_URL + "/", StandardCharsets.UTF_8);
            page.navigate("/id?source=russpass&redirect-url=" + redirect);
            PageActions.acceptCookies(page);

            Locator mosRu = mosRuControl(page);
            assertThat(mosRu).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));
            mosRu.click();

            Page loginPage = waitForLoginPage(context, page);
            assertThat(mosRuLoginField(loginPage))
                    .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));
            fillMosRuCredentials(loginPage, login, password);

            if (waitForSmsPrompt(context, 15_000)) {
                System.out.println();
                System.out.println(">>> Введите SMS-код в окне браузера и нажмите «Подтвердить» (ждём до 5 минут)...");
                System.out.println();
            }

            Page portal = waitForPortalOutsideId(context, 300_000);
            assertThat(portal.locator("a[href=\"/loyalty\"]"))
                    .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));

            context.storageState(new BrowserContext.StorageStateOptions().setPath(TestSettings.AUTH_FILE));
            System.out.println(">>> Авторизация: сессия сохранена в " + TestSettings.AUTH_FILE);
        }
    }

    private static Locator mosRuControl(Page page) {
        Locator button = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Mos.ru"));
        if (button.count() > 0) {
            return button.first();
        }
        return page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Mos.ru")).first();
    }

    private static void fillMosRuCredentials(Page loginPage, String login, String password) {
        System.out.println(">>> Авторизация: форма Mos.ru " + loginPage.url());
        mosRuLoginField(loginPage).fill(login);
        mosRuPasswordField(loginPage).fill(password);
        mosRuSubmit(loginPage).click();
    }

    private static Locator mosRuLoginField(Page page) {
        Locator byRole = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(LOGIN_FIELD));
        if (byRole.count() > 0) {
            return byRole.first();
        }
        return page.locator("#login, input[name='login'], input[name='username']").first();
    }

    private static Locator mosRuPasswordField(Page page) {
        Locator byRole = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Пароль"));
        if (byRole.count() > 0) {
            return byRole.first();
        }
        return page.locator("#password, input[type='password'][name='password'], input[type='password']").first();
    }

    private static Locator mosRuSubmit(Page page) {
        Locator byRole = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Войти"));
        if (byRole.count() > 0) {
            return byRole.last();
        }
        return page.locator("#bind, button[type='submit']").last();
    }

    private static Page waitForLoginPage(BrowserContext context, Page fallback) {
        long deadline = System.currentTimeMillis() + 45_000;
        while (System.currentTimeMillis() < deadline) {
            for (Page candidate : context.pages()) {
                if (candidate.isClosed()) {
                    continue;
                }
                if (isMosRuLoginUrl(candidate.url()) || hasMosRuLoginForm(candidate)) {
                    return candidate;
                }
            }
            fallback.waitForTimeout(250);
        }
        throw new AssertionError("Не дождались формы входа Mos.ru (login-tech.mos.ru). Открыто: "
                + context.pages().stream()
                .filter(p -> !p.isClosed())
                .map(Page::url)
                .reduce((a, b) -> a + " | " + b)
                .orElse("(нет вкладок)"));
    }

    private static boolean isMosRuLoginUrl(String url) {
        String lower = url.toLowerCase();
        return lower.contains("login-tech.mos.ru") || lower.contains("login.mos.ru");
    }

    private static boolean hasMosRuLoginForm(Page page) {
        try {
            return page.locator("#login, #password, input[name='login']").count() > 0
                    || page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(LOGIN_FIELD)).count() > 0;
        } catch (PlaywrightException e) {
            return false;
        }
    }

    private static boolean waitForSmsPrompt(BrowserContext context, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Page candidate : context.pages()) {
                if (candidate.isClosed()) {
                    continue;
                }
                Locator sms = candidate.getByText(SMS_TEXT);
                try {
                    if (sms.count() > 0 && sms.first().isVisible()) {
                        return true;
                    }
                } catch (PlaywrightException ignored) {
                    // вкладка закрылась во время проверки
                }
            }
            Page any = firstOpenPage(context);
            if (any != null) {
                any.waitForTimeout(250);
            }
        }
        return false;
    }

    /**
     * Аналог TS-предиката в page.waitForURL: ждём возврата на портал вне раздела /id.
     * Опрашиваем URL из Java, т.к. при переходах между mos.ru и порталом контекст страницы пересоздаётся.
     */
    static void waitForPortalOutsideId(Page page, int timeoutMs) {
        waitForPortalOutsideId(page.context(), timeoutMs);
    }

    static Page waitForPortalOutsideId(BrowserContext context, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            for (Page candidate : context.pages()) {
                if (!candidate.isClosed() && isPortalOutsideId(candidate.url())) {
                    return candidate;
                }
            }
            Page any = firstOpenPage(context);
            if (any != null) {
                any.waitForTimeout(500);
            }
        }
        String urls = context.pages().stream()
                .filter(p -> !p.isClosed())
                .map(Page::url)
                .reduce((a, b) -> a + " | " + b)
                .orElse("(нет открытых вкладок)");
        throw new AssertionError("Не дождались возврата на портал вне /id, текущие URL: " + urls);
    }

    private static Page firstOpenPage(BrowserContext context) {
        for (Page candidate : context.pages()) {
            if (!candidate.isClosed()) {
                return candidate;
            }
        }
        return null;
    }

    public static boolean isPortalOutsideId(String rawUrl) {
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
