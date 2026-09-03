package ru.russpass.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiverTripPurchaseTest extends PlaywrightTestBase {
    private static final Pattern TIME_SLOT = Pattern.compile("\\d{1,2}:\\d{2}\\s*—\\s*\\d{1,2}:\\d{2}");
    private static final Pattern BUY_TICKETS = Pattern.compile("Купить билеты \\d+ билет", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAY_TICKETS = Pattern.compile("Оплатить .*билет", Pattern.CASE_INSENSITIVE);
    private static final Pattern CARD_PAY = Pattern.compile("Оплатить \\d");
    private static final Pattern PHONE_FIELD = Pattern.compile("Телефон|\\+7", Pattern.CASE_INSENSITIVE);
    private static final Pattern EMAIL_FIELD = Pattern.compile("Почта", Pattern.CASE_INSENSITIVE);
    private static final Pattern MOSCOW_TRIPS = Pattern.compile("Речные прогулки в Москве", Pattern.CASE_INSENSITIVE);
    private static final Pattern ZARYADYE = Pattern.compile("Зарядье", Pattern.CASE_INSENSITIVE);
    private static final Pattern CHOOSE = Pattern.compile("Выбрать", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANK_CARD = Pattern.compile("Банковская карта", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAY_PATH = Pattern.compile("/pay");

    @Test
    @DisplayName("покупка речной прогулки Зарядье картой")
    void purchaseZaryadyeRiverTripByCard() {
        String lastName = Env.get("RUSSPASS_LAST_NAME");
        String firstName = Env.get("RUSSPASS_FIRST_NAME");
        String phone = Env.get("RUSSPASS_PHONE");
        String email = Env.get("RUSSPASS_EMAIL");
        String cardNumber = Env.get("CARD_NUMBER", "").replaceAll("\\s+", "");
        String cardExpiry = Env.get("CARD_EXPIRY", "");
        String cardCvc = Env.get("CARD_CVC", "");

        Assumptions.assumeFalse(
                cardNumber.isBlank() || cardExpiry.isBlank() || cardCvc.isBlank(),
                "Заполните CARD_NUMBER, CARD_EXPIRY, CARD_CVC в .env"
        );

        page.navigate("/");
        PageActions.acceptCookies(page);

        // Сессия должна уже быть из AuthSession
        assertThat(page.locator("a[href=\"/loyalty\"]"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));

        page.navigate("/river-trips");
        assertThat(page.getByText(MOSCOW_TRIPS))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));

        Locator tripLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName(ZARYADYE)).first();
        assertThat(tripLink).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));
        tripLink.scrollIntoViewIfNeeded();
        String href = tripLink.getAttribute("href");
        assertTrue(href != null && !href.isBlank(), "У карточки «Зарядье» нет href");
        page.navigate(URI.create(page.url()).resolve(href).toString());

        Locator timeField = page.getByText("Время — Судно", new Page.GetByTextOptions().setExact(true));
        assertThat(timeField).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));
        timeField.click();

        Locator timeSheet = page.locator("[data-rsbs-content=\"true\"]");
        assertThat(timeSheet).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10_000));
        timeSheet.getByText(TIME_SLOT).first().click();
        timeSheet.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(CHOOSE)).click();

        // Ждём закрытия шторки выбора рейса, иначе она перехватывает клик по «Пассажиры»
        assertThat(page.locator("[data-rsbs-backdrop=\"true\"]"))
                .hasCount(0, new LocatorAssertions.HasCountOptions().setTimeout(15_000));
        assertThat(page.getByText("Время — Судно", new Page.GetByTextOptions().setExact(true))).hasCount(0);

        PageActions.increaseAdultTickets(page);

        Locator buyButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(BUY_TICKETS));
        assertThat(buyButton).isVisible();
        buyButton.click();

        assertThat(page).hasURL(PAY_PATH);
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Фамилия")).fill(lastName);
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Имя")).fill(firstName);
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(PHONE_FIELD)).fill(phone);
        page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(EMAIL_FIELD)).fill(email);

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(PAY_TICKETS)).click();
        assertThat(page.getByText("Способ оплаты"))
                .isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(30_000));

        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(BANK_CARD)).click();
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(CARD_PAY)).click();

        PaymentFlow.fillCardAndPay(page, cardNumber, cardExpiry, cardCvc);
        PaymentFlow.expectSuccess(page);
    }
}
