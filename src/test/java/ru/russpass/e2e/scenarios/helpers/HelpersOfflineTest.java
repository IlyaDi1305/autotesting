package ru.russpass.e2e.scenarios.helpers;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Route;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import ru.russpass.e2e.helpers.AuthSession;
import ru.russpass.e2e.helpers.Env;
import ru.russpass.e2e.helpers.PageActions;
import ru.russpass.e2e.helpers.PaymentFlow;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("helpers")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HelpersOfflineTest {
    private static final Pattern OTP_LIKE = Pattern.compile("код|пароль|sms|подтвержд", Pattern.CASE_INSENSITIVE);

    Playwright playwright;
    Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    void launch() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    void close() {
        playwright.close();
    }

    @BeforeEach
    void newPage() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closePage() {
        context.close();
    }

    @Test
    void envReadsCyrillicValuesFromDotEnv() {
        assertFalse(Env.get("RUSSPASS_LAST_NAME").isBlank(), "RUSSPASS_LAST_NAME должен быть заполнен в .env");
        assertFalse(Env.get("RUSSPASS_FIRST_NAME").isBlank(), "RUSSPASS_FIRST_NAME должен быть заполнен в .env");
        assertEquals("fallback", Env.get("NO_SUCH_KEY", "fallback"));
    }

    @Test
    void acceptCookiesClicksBannerAndIsSafeWhenAbsent() {
        page.setContent("<button onclick=\"window.clicked=true\">Хорошо</button>");
        PageActions.acceptCookies(page);
        assertTrue((Boolean) page.evaluate("() => window.clicked === true"));

        page.setContent("<div>без баннера</div>");
        PageActions.acceptCookies(page); // не должно бросать
    }

    @Test
    void typeIntoFiresKeyEventsAndReplacesExistingValue() {
        page.setContent("""
                <input id="cardNumber" value="0000">
                <script>
                  window.keys = 0;
                  document.getElementById('cardNumber')
                    .addEventListener('keydown', () => window.keys++);
                </script>
                """);
        PageActions.typeInto(page, "#cardNumber", "4111111111111111");
        assertEquals("4111111111111111", page.locator("#cardNumber").inputValue());
        assertTrue(((Number) page.evaluate("() => window.keys")).intValue() >= 16);
    }

    @Test
    void increaseAdultTicketsClicksPlusInAdultRow() {
        page.setContent("""
                <div>Пассажиры</div>
                <div data-rsbs-content="true">
                  <div>
                    <div><span>Взрослый</span><span>1000 ₽</span>
                      <button onclick="window.minus=true">−</button>
                      <button onclick="window.plus=true">+</button>
                    </div>
                  </div>
                </div>
                """);
        PageActions.increaseAdultTickets(page);
        assertTrue((Boolean) page.evaluate("() => window.plus === true"), "клик по «+» взрослого билета");
        assertTrue((Boolean) page.evaluate("() => window.minus === undefined"), "«−» не должен нажиматься");
    }

    @Test
    void increaseAdultTicketsFallsBackToThirdSheetButton() {
        page.setContent("""
                <div>Пассажиры</div>
                <div data-rsbs-content="true">
                  <button>Назад</button>
                  <button>−</button>
                  <button onclick="window.fallback=true">+</button>
                </div>
                """);
        PageActions.increaseAdultTickets(page);
        assertTrue((Boolean) page.evaluate("() => window.fallback === true"), "fallback-клик по третьей кнопке");
    }

    @Test
    void russianRegexLocatorsMatchRealLabels() {
        page.setContent("""
                <button>Купить билеты 2 билета</button>
                <button>Оплатить 2 билета</button>
                <button>Банковская карта</button>
                <button>Оплатить 1500 ₽</button>
                <input type="text" aria-label="Введите код из SMS">
                <a href="/trip">Речная прогулка Зарядье</a>
                """);

        Pattern buyTickets = Pattern.compile("Купить билеты \\d+ билет", Pattern.CASE_INSENSITIVE);
        Pattern payTickets = Pattern.compile("Оплатить .*билет", Pattern.CASE_INSENSITIVE);
        Pattern bankCard = Pattern.compile("Банковская карта", Pattern.CASE_INSENSITIVE);
        Pattern cardPay = Pattern.compile("Оплатить \\d");
        Pattern zaryadye = Pattern.compile("Зарядье", Pattern.CASE_INSENSITIVE);

        assertEquals(1, page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(buyTickets)).count());
        assertEquals(1, page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(payTickets)).count());
        assertEquals(1, page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(bankCard)).count());
        assertEquals(2, page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(cardPay)).count());
        assertEquals(1, page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(OTP_LIKE)).count());

        Locator link = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(zaryadye)).first();
        assertEquals("/trip", link.getAttribute("href"));
    }

    @Test
    void timeSlotRegexMatchesSheetRow() {
        page.setContent("""
                <div data-rsbs-content="true">
                  <div>Речной трамвай</div>
                  <div>10:30 — 11:45</div>
                  <button>Выбрать</button>
                </div>
                """);
        Locator sheet = page.locator("[data-rsbs-content=\"true\"]");
        Locator slot = sheet.getByText(Pattern.compile("\\d{1,2}:\\d{2}\\s*—\\s*\\d{1,2}:\\d{2}")).first();
        assertEquals("10:30 — 11:45", slot.innerText().trim());
        assertEquals(1, sheet.getByRole(AriaRole.BUTTON,
                new Locator.GetByRoleOptions().setName(Pattern.compile("Выбрать", Pattern.CASE_INSENSITIVE))).count());
    }

    @Test
    void fillCardAndPayReachesSuccessThroughWaiting() {
        routeFakePayecom(page, Scenario.SUCCESS);
        page.navigate("https://payecom.ru/pay");

        PaymentFlow.fillCardAndPay(page, "4111111111111111", "12/30", "123");
        PaymentFlow.expectSuccess(page);

        assertTrue(page.url().contains("status=success"), "итоговый URL: " + page.url());
    }

    @Test
    void fillCardAndPayFailsOnStatusError() {
        routeFakePayecom(page, Scenario.ERROR);
        page.navigate("https://payecom.ru/pay");

        AssertionError error = assertThrows(AssertionError.class,
                () -> PaymentFlow.fillCardAndPay(page, "4111111111111111", "12/30", "123"));
        assertTrue(error.getMessage().contains("status=error"), error.getMessage());
    }

    @Test
    void fillCardAndPayDetects3dsAndRequiresCode() {
        routeFakePayecom(page, Scenario.THREE_DS);
        page.navigate("https://payecom.ru/pay");

        // CARD_3DS_CODE в .env пустой → шаг 3DS обнаружен и требует код
        AssertionError error = assertThrows(AssertionError.class,
                () -> PaymentFlow.fillCardAndPay(page, "4111111111111111", "12/30", "123"));
        assertTrue(error.getMessage().contains("CARD_3DS_CODE"), error.getMessage());
    }

    @Test
    void portalUrlPredicateMatchesTsSemantics() {
        assertTrue(AuthSession.isPortalOutsideId("https://portal.dev01.russpass.dev/"));
        assertTrue(AuthSession.isPortalOutsideId("https://portal.dev01.russpass.dev/river-trips?x=1"));
        assertFalse(AuthSession.isPortalOutsideId("https://portal.dev01.russpass.dev/id?source=russpass"));
        assertFalse(AuthSession.isPortalOutsideId("https://login.mos.ru/sps/login/methods/password"));
        assertFalse(AuthSession.isPortalOutsideId("about:blank"));
    }

    @Test
    void waitForOutcomeClassifiesUrls() {
        page.route(Pattern.compile(".*"), route -> route.fulfill(new Route.FulfillOptions()
                .setContentType("text/html; charset=utf-8")
                .setBody("<meta charset=\"utf-8\"><div>Почти готово...</div>")));
        Locator otp = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(OTP_LIKE));

        page.navigate("https://portal.example/payment/result?status=waiting");
        assertEquals(PaymentFlow.Outcome.WAITING, PaymentFlow.waitForOutcome(page, otp, 5_000));

        page.navigate("https://portal.example/payment/result?status=error");
        assertEquals(PaymentFlow.Outcome.ERROR, PaymentFlow.waitForOutcome(page, otp, 5_000));

        page.navigate("https://portal.example/payment/result?status=success");
        assertEquals(PaymentFlow.Outcome.SUCCESS, PaymentFlow.waitForOutcome(page, otp, 5_000));

        // как и в исходном TS-тесте, промежуточный success-sber распознаётся как success
        page.navigate("https://portal.example/payment/success-sber?status=waiting");
        assertEquals(PaymentFlow.Outcome.SUCCESS, PaymentFlow.waitForOutcome(page, otp, 5_000));
    }

    private enum Scenario {
        SUCCESS,
        ERROR,
        THREE_DS
    }

    /**
     * Симулирует Payecom: кнопка «Оплатить» включается только после key events во всех трёх полях,
     * далее цепочка success-sber?status=waiting → status=success/error либо экран 3DS.
     */
    private static void routeFakePayecom(Page page, Scenario scenario) {
        String next = switch (scenario) {
            case SUCCESS -> "https://portal.example/payment/success-sber?status=waiting";
            case ERROR -> "https://portal.example/payment/result?status=error";
            case THREE_DS -> "https://payecom.ru/3ds";
        };
        String payPage = """
                <meta charset="utf-8">
                <input id="cardNumber"><input id="expiryDate"><input id="cvc">
                <button id="pay" disabled>Оплатить</button>
                <script>
                  const ids = ['cardNumber', 'expiryDate', 'cvc'];
                  const touched = new Set();
                  ids.forEach((id) => {
                    const el = document.getElementById(id);
                    el.addEventListener('keydown', () => touched.add(id));
                    el.addEventListener('input', () => {
                      if (touched.size === 3 && ids.every((i) => document.getElementById(i).value.length > 2)) {
                        document.getElementById('pay').disabled = false;
                      }
                    });
                  });
                  document.getElementById('pay').addEventListener('click', () => {
                    location.href = '%s';
                  });
                </script>
                """.formatted(next);
        String threeDsPage = """
                <meta charset="utf-8">
                <input type="text" aria-label="Введите код подтверждения из SMS">
                <button>Подтвердить</button>
                """;
        String waitingPage = """
                <meta charset="utf-8">
                <div>Почти готово...</div>
                <script>
                  setTimeout(() => { location.href = 'https://portal.example/payment/result?status=%s'; }, 400);
                </script>
                """.formatted(scenario == Scenario.SUCCESS ? "success" : "error");

        page.route(Pattern.compile(".*"), route -> {
            String url = route.request().url();
            String body;
            if (url.contains("payecom.ru/3ds")) {
                body = threeDsPage;
            } else if (url.contains("payecom.ru")) {
                body = payPage;
            } else if (url.contains("status=waiting")) {
                body = waitingPage;
            } else {
                body = "<meta charset=\"utf-8\"><div>Готово</div>";
            }
            route.fulfill(new Route.FulfillOptions()
                    .setContentType("text/html; charset=utf-8")
                    .setBody(body));
        });
    }
}
