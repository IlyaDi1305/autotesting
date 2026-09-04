package ru.russpass.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class PaymentFlow {
    static final Pattern PAYECOM = Pattern.compile("payecom\\.ru", Pattern.CASE_INSENSITIVE);
    static final Pattern STATUS_SUCCESS = Pattern.compile("status=success", Pattern.CASE_INSENSITIVE);
    static final Pattern STATUS_ERROR = Pattern.compile("status=error", Pattern.CASE_INSENSITIVE);

    private static final Pattern OTP_NAME = Pattern.compile("код|пароль|sms|подтвержд", Pattern.CASE_INSENSITIVE);
    private static final Pattern OTP_SUBMIT = Pattern.compile("подтвердить|отправ|продолжить|ok|далее", Pattern.CASE_INSENSITIVE);
    private static final int PAYMENT_TIMEOUT_MS = 180_000;

    private PaymentFlow() {
    }

    public static void fillCardAndPay(Page page, String cardNumber, String cardExpiry, String cardCvc) {
        page.waitForURL(PAYECOM, new Page.WaitForURLOptions().setTimeout(60_000));
        page.locator("#cardNumber").waitFor(new Locator.WaitForOptions()
                .setState(WaitForSelectorState.VISIBLE)
                .setTimeout(30_000));

        String expiryDigits = cardExpiry.replaceAll("\\D", ""); // 05/35 -> 0535, маска сама поставит /

        PageActions.typeInto(page, "#cardNumber", cardNumber);
        PageActions.typeInto(page, "#expiryDate", expiryDigits);
        PageActions.typeInto(page, "#cvc", cardCvc);

        Locator payButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Оплатить"));
        try {
            assertThat(payButton).isEnabled(new LocatorAssertions.IsEnabledOptions().setTimeout(20_000));
        } catch (AssertionError e) {
            throw new AssertionError(
                    "Кнопка «Оплатить» не активировалась — проверьте данные карты в .env", e);
        }
        payButton.click();

        // Нельзя просто ждать 3DS 15с — за это время проскочит success-sber.
        // Ждём параллельно: 3DS / success / waiting / error.
        Locator otp = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName(OTP_NAME));
        Outcome outcome = waitForOutcome(page, otp, PAYMENT_TIMEOUT_MS);

        if (outcome == Outcome.ERROR) {
            throw new AssertionError("Оплата завершилась со status=error");
        }

        if (outcome == Outcome.OTP) {
            String card3dsCode = Env.get("CARD_3DS_CODE");
            assertFalse(card3dsCode.isBlank(), "Заполните CARD_3DS_CODE в .env для 3DS");
            otp.fill(card3dsCode);
            page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(OTP_SUBMIT)).click();
        }
    }

    public static Outcome waitForOutcome(Page page, Locator otp, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            String url = page.url();
            if (STATUS_ERROR.matcher(url).find()) {
                return Outcome.ERROR;
            }
            if (url.matches("(?i).*(/payment/success|status=success).*")) {
                return Outcome.SUCCESS;
            }
            if (url.matches("(?i).*status=waiting.*")) {
                return Outcome.WAITING;
            }
            if (otp.count() > 0 && otp.first().isVisible()) {
                return Outcome.OTP;
            }
            page.waitForTimeout(200);
        }
        throw new AssertionError("Не дождались исхода оплаты за " + timeoutMs + " мс");
    }

    public static void expectSuccess(Page page) {
        // Цепочка: payecom → /payment/success-sber → status=waiting («Почти готово...») → status=success
        page.waitForURL(STATUS_SUCCESS, new Page.WaitForURLOptions().setTimeout(PAYMENT_TIMEOUT_MS));

        String url = page.url();
        assertFalse(STATUS_ERROR.matcher(url).find(), "Оплата завершилась ошибкой (status=error): " + url);
        assertThat(page.getByText("Whitelabel Error Page")).hasCount(0);
        assertTrue(STATUS_SUCCESS.matcher(url).find(), "Прогулка не была куплена: нет status=success, URL: " + url);
    }

    public enum Outcome {
        SUCCESS,
        WAITING,
        ERROR,
        OTP
    }
}
