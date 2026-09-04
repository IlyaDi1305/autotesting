package ru.russpass.e2e.scenarios.rivertrip;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import ru.russpass.e2e.base.PlaywrightTestBase;
import ru.russpass.e2e.helpers.Env;
import ru.russpass.e2e.helpers.PageActions;
import ru.russpass.e2e.helpers.PaymentFlow;

import java.net.URI;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

@Tag("rivertrip")
public class RiverTripPurchaseTest extends PlaywrightTestBase {

    private static final Pattern TIME_SLOT =
            Pattern.compile("\\d{1,2}:\\d{2}\\s*—\\s*\\d{1,2}:\\d{2}");

    private static final Pattern BUY_TICKETS =
            Pattern.compile(
                    "Купить билеты \\d+ билет",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern PAY_TICKETS =
            Pattern.compile(
                    "Оплатить .*билет",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern CARD_PAY =
            Pattern.compile(
                    "Оплатить \\d",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern MOSCOW_TRIPS =
            Pattern.compile(
                    "Речные прогулки в Москве",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern CHOOSE =
            Pattern.compile(
                    "Выбрать",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern BANK_CARD =
            Pattern.compile(
                    "Банковская карта",
                    Pattern.CASE_INSENSITIVE
            );

    // Сюда можно добавить несколько тестов
    @Test
    void allTests() {
        purchaseCrimeanBridgeToRussia();
        purchaseKlenovyToKitayGorod();
        purchaseParkGorkogo();
    }

    /*
     * ============================================================
     * 01
     * ============================================================
     */

    @Test
    void purchaseCrimeanBridgeToRussia() {
        purchaseRiverTrip(
                RiverTrip.CRIMEAN_BRIDGE_TO_RUSSIA
        );
    }

    /*
     * ============================================================
     * 02
     * ============================================================
     */

    @Test
    void purchaseRussiaToCrimeanBridge() {
        purchaseRiverTrip(
                RiverTrip.RUSSIA_TO_CRIMEAN_BRIDGE
        );
    }

    /*
     * ============================================================
     * 03
     * ============================================================
     */

    @Test
    void purchaseKlenovyToKitayGorod() {
        purchaseRiverTrip(
                RiverTrip.KLENOVY_TO_KITAY_GOROD
        );
    }

    /*
     * ============================================================
     * 04
     * ============================================================
     */

    @Test
    void purchaseParkGorkogo() {
        purchaseRiverTrip(
                RiverTrip.PARK_GORKOGO
        );
    }

    /*
     * ============================================================
     * 05
     * ============================================================
     */

    @Test
    void purchaseKlenovyToKievskyFirst() {
        purchaseRiverTrip(
                RiverTrip.KLENOVY_TO_KIEVSKY_1
        );
    }

    /*
     * ============================================================
     * 06
     * ============================================================
     */

    @Test
    void purchaseSevernyExpress() {
        purchaseRiverTrip(
                RiverTrip.SEVERNY_EXPRESS
        );
    }

    /*
     * ============================================================
     * 07
     * ============================================================
     */

    @Test
    void purchaseSeverny() {
        purchaseRiverTrip(
                RiverTrip.SEVERNY
        );
    }

    /*
     * ============================================================
     * 08
     * ============================================================
     */

    @Test
    void purchaseSrvZakharkovo() {
        purchaseRiverTrip(
                RiverTrip.SRV_ZAKHARKOVO
        );
    }

    /*
     * ============================================================
     * 09
     * ============================================================
     */

    @Test
    void purchaseSrvKhimki() {
        purchaseRiverTrip(
                RiverTrip.SRV_KHIMKI
        );
    }

    /*
     * ============================================================
     * 10
     * ============================================================
     */

    @Test
    void purchaseHistorical() {
        purchaseRiverTrip(
                RiverTrip.HISTORICAL
        );
    }

    /*
     * ============================================================
     * 11
     * ============================================================
     */

    @Test
    void purchaseKolomenskyExpress() {
        purchaseRiverTrip(
                RiverTrip.KOLOMENSKY_EXPRESS
        );
    }

    /*
     * ============================================================
     * 12
     * ============================================================
     */

    @Test
    void purchaseCrimeanBridgeRound() {
        purchaseRiverTrip(
                RiverTrip.CRIMEAN_BRIDGE_ROUND
        );
    }

    /*
     * ============================================================
     * 13
     * ============================================================
     */

    @Test
    void purchaseRussiaRound() {
        purchaseRiverTrip(
                RiverTrip.RUSSIA_ROUND
        );
    }

    /*
     * ============================================================
     * 14
     * ============================================================
     */

    @Test
    void purchaseTretyakovskyRound() {
        purchaseRiverTrip(
                RiverTrip.TRETYAKOVSKY_ROUND
        );
    }

    /*
     * ============================================================
     * 15
     * ============================================================
     */

    @Test
    void purchaseKlenovyToKievskySecond() {
        purchaseRiverTrip(
                RiverTrip.KLENOVY_TO_KIEVSKY_2
        );
    }

    /*
     * ============================================================
     * 16
     * ============================================================
     */

    @Test
    void purchaseSevernyRiverStationRound() {
        purchaseRiverTrip(
                RiverTrip.SEVERNY_RIVER_STATION_ROUND
        );
    }

    /*
     * ============================================================
     * 17
     * ============================================================
     */

    @Test
    void purchaseZaryadyeRound() {
        purchaseRiverTrip(
                RiverTrip.ZARYADYE_ROUND
        );
    }

    /*
     * ============================================================
     * 18
     * ============================================================
     */

    @Test
    void purchaseRadissonRoyal() {
        purchaseRiverTrip(
                RiverTrip.RADISSON_ROYAL
        );
    }

    /*
     * ============================================================
     * COMMON PURCHASE FLOW
     * ============================================================
     */

    private void purchaseRiverTrip(RiverTrip trip) {

        String lastName = Env.get("RUSSPASS_LAST_NAME");
        String firstName = Env.get("RUSSPASS_FIRST_NAME");
        String phone = Env.get("RUSSPASS_PHONE");
        String email = Env.get("RUSSPASS_EMAIL");

        String cardNumber = Env.get("CARD_NUMBER");
        String cardExpiry = Env.get("CARD_EXPIRY");
        String cardCvc = Env.get("CARD_CVC");

        org.junit.jupiter.api.Assumptions.assumeTrue(
                cardNumber != null && !cardNumber.isBlank(),
                "CARD_NUMBER не задан"
        );

        org.junit.jupiter.api.Assumptions.assumeTrue(
                cardExpiry != null && !cardExpiry.isBlank(),
                "CARD_EXPIRY не задан"
        );

        org.junit.jupiter.api.Assumptions.assumeTrue(
                cardCvc != null && !cardCvc.isBlank(),
                "CARD_CVC не задан"
        );

        System.out.println();
        System.out.println("====================================================");
        System.out.println("START RIVER TRIP TEST");
        System.out.println("Name : " + trip.name());
        System.out.println("ID   : " + trip.id());
        System.out.println("====================================================");

        /*
         * --------------------------------------------------------
         * 1. Открываем главную
         * --------------------------------------------------------
         */

        page.navigate("/");

        PageActions.acceptCookies(page);

        /*
         * --------------------------------------------------------
         * 2. Проверяем авторизацию
         * --------------------------------------------------------
         */

        assertThat(
                page.locator("a[href=\"/loyalty\"]")
        ).isVisible();

        /*
         * --------------------------------------------------------
         * 3. Открываем речные прогулки
         * --------------------------------------------------------
         */

        page.navigate("/river-trips");

        assertThat(
                page.getByText(MOSCOW_TRIPS)
        ).isVisible();

        /*
         * --------------------------------------------------------
         * 4. Ищем нужный маршрут по ID
         * --------------------------------------------------------
         */

        Locator tripLink = page.locator(
                "a[href*='" + trip.id() + "']"
        ).first();

        assertThat(tripLink)
                .isVisible();

        String href = tripLink.getAttribute("href");

        if (href == null || href.isBlank()) {
            throw new AssertionError(
                    "У маршрута отсутствует href: " + trip
            );
        }

        System.out.println(
                "Found route href: " + href
        );

        /*
         * --------------------------------------------------------
         * 5. Переходим на страницу маршрута
         * --------------------------------------------------------
         */

        String tripUrl = URI
                .create(page.url())
                .resolve(href)
                .toString();

        System.out.println("tripUrl - " + tripUrl);

        page.navigate(tripUrl);

        /*
         * --------------------------------------------------------
         * 6. Выбираем причал или время/судно (если нужно)
         * --------------------------------------------------------
         */

        boolean selectionMade = false;

// Проверяем, есть ли поле выбора причала
        if (page.getByText("Причал отправления").count() > 0) {
            System.out.println("Найден выбор причала, открываем список...");
            page.getByText("Причал отправления").click();
            page.waitForTimeout(500);

            // Ждём появления модального окна
            page.locator("[data-rsbs-content='true']").waitFor();

            // Находим все видимые div внутри модалки с непустым текстом
            Locator options = page.locator("[data-rsbs-content='true'] div:visible:not(:empty)");
            int count = options.count();
            System.out.println("Найдено видимых элементов: " + count);

            Locator selected = null;
            for (int i = 0; i < count; i++) {
                Locator option = options.nth(i);
                String text = option.textContent().trim();
                // Пропускаем служебные элементы и родительские контейнеры с длинным текстом (> 50 символов)
                if (!text.contains("Назад") && !text.contains("Причал отправления")
                        && !text.isEmpty() && text.length() < 50) {
                    selected = option;
                    break;
                }
            }

            if (selected != null) {
                System.out.println("Кликаем на причал отправления: " + selected.textContent());
                selected.click();
                System.out.println("Причал выбран: " + selected.textContent());
                selectionMade = true;
            } else {
                System.out.println("Не найден подходящий вариант причала");
            }
            clickSelectButton();
            page.getByText("Причал прибытия").click();

            selected = null;
            for (int i = 0; i < count; i++) {
                Locator option = options.nth(i);
                String text = option.textContent().trim();
                // Пропускаем служебные элементы и родительские контейнеры с длинным текстом (> 50 символов)
                if (!text.contains("Назад") && !text.contains("Причал прибытия") && !text.contains("Причал отправления")
                        && !text.isEmpty() && text.length() < 50) {
                    selected = option;
                    break;
                }
            }

            if (selected != null) {
                System.out.println("Кликаем на причал прибытия: " + selected.textContent());
                selected.click();
                System.out.println("Причал выбран прибытия: " + selected.textContent());
                selectionMade = true;
            } else {
                System.out.println("Не найден подходящий вариант причала");
            }
            clickSelectButton();
        }

        System.out.println("Найден выбор времени/судна, открываем список...");
        page.getByText("Время — Судно").click();
        page.waitForTimeout(500);
        // Берём первый видимый слот по паттерну TIME_SLOT
        Locator timeSlot = page.getByText(TIME_SLOT).first();
        if (timeSlot.count() > 0 && timeSlot.isVisible()) {
            timeSlot.click(new Locator.ClickOptions().setForce(true));
            System.out.println("Время выбрано: " + timeSlot.textContent());
            clickSelectButton();
        } else {
            System.out.println("Слоты времени не найдены");
        }

        if (!selectionMade) {
            System.out.println("Ни причал, ни время не требуют выбора (уже выбраны или не нужны)");
        }

// Даём время на обновление состояния
        page.waitForTimeout(500);


        /*
         * --------------------------------------------------------
         * 8. Дополнительная задержка перед следующим шагом
         * --------------------------------------------------------
         */

        page.waitForTimeout(500);
        /*
         * --------------------------------------------------------
         * 9. Добавляем 1 взрослый билет
         * --------------------------------------------------------
         */

        PageActions.increaseAdultTickets(page);

        /*
         * --------------------------------------------------------
         * 10. Купить билеты
         * --------------------------------------------------------
         */

        Locator buyButton = page.getByRole(
                com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName(BUY_TICKETS)
        );

        assertThat(buyButton)
                .isVisible();

        buyButton.click();

        /*
         * --------------------------------------------------------
         * 11. Проверяем переход на оплату
         * --------------------------------------------------------
         */

        page.waitForURL(
                Pattern.compile(".*/pay.*")
        );

        assertThat(
                page
        ).hasURL(
                Pattern.compile(".*/pay.*")
        );

        /*
         * --------------------------------------------------------
         * 12. Заполняем данные пассажира
         * --------------------------------------------------------
         */

        System.out.println("Начинаем заполнение данных - Фамилия");
        PageActions.typeInto(
                page.getByLabel("Фамилия"),
                lastName
        );

        System.out.println("Начинаем заполнение данных - Имя");
        PageActions.typeInto(
                page.getByLabel("Имя"),
                firstName
        );

        System.out.println("Начинаем заполнение данных - Телефон");
        PageActions.typeInto(
                page.locator(
                        "input[type='tel'], " +
                                "input[name='phone'], " +
                                "input[placeholder*='Телефон'], " +
                                "input[aria-label*='Телефон'], " +
                                "input[data-testid='phone-input']"
                ).first(),
                phone
        );

        System.out.println("Начинаем заполнение данных - Почта");
        PageActions.typeInto(
                page.getByLabel(
                        Pattern.compile(
                                "Почта",
                                Pattern.CASE_INSENSITIVE
                        )
                ),
                email
        );

        /*
         * --------------------------------------------------------
         * 13. Переходим к оплате
         * --------------------------------------------------------
         */

        System.out.println("Переходим к оплате");
        Locator payTicketsButton = page.getByRole(
                com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName(PAY_TICKETS)
        );

        assertThat(payTicketsButton).isVisible();

        payTicketsButton.click();

        /*
         * --------------------------------------------------------
         * 14. Способ оплаты
         * --------------------------------------------------------
         */

        assertThat(
                page.getByText("Способ оплаты")
        ).isVisible();

        /*
         * --------------------------------------------------------
         * 15. Банковская карта
         * --------------------------------------------------------
         */

        System.out.println("Выбираем банковскую карту");
        Locator bankCard = page.getByText(
                BANK_CARD
        ).first();

        assertThat(bankCard).isVisible();

        bankCard.click();

        /*
         * --------------------------------------------------------
         * 16. Оплата картой
         * --------------------------------------------------------
         */

        System.out.println("CardPayButton");
        Locator cardPayButton = page.getByRole(
                com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions()
                        .setName(CARD_PAY)
        );

        assertThat(cardPayButton).isVisible();

        cardPayButton.click();

        /*
         * --------------------------------------------------------
         * 17. Заполняем карту
         * --------------------------------------------------------
         */

        System.out.println("Заполнение карты");

        PaymentFlow.fillCardAndPay(
                page,
                cardNumber,
                cardExpiry,
                cardCvc
        );

        /*
         * --------------------------------------------------------
         * 18. Проверяем успешную оплату
         * --------------------------------------------------------
         */

        PaymentFlow.expectSuccess(page);

        System.out.println();
        System.out.println("====================================================");
        System.out.println("SUCCESS");
        System.out.println("Name : " + trip.name());
        System.out.println("ID   : " + trip.id());
        System.out.println("====================================================");
        System.out.println();
    }

    private void clickSelectButton() {
        // Ищем кнопку «Выбрать»
        Locator chooseButton = page.getByRole(
                com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(CHOOSE)
        ).last();

// Проверяем, есть ли кнопка на странице (с таймаутом 2 секунды)
        try {
            chooseButton.waitFor(new Locator.WaitForOptions()
                    .setTimeout(2000)
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE));

            // Если кнопка видима, кликаем
            System.out.println("Кнопка «Выбрать» найдена, кликаем");
            chooseButton.click(new Locator.ClickOptions().setForce(true));
            System.out.println("Кнопка «Выбрать» нажата");

            // Ждём, пока модальное окно закроется (если оно было)
            try {
                page.locator("[data-rsbs-content='true']").waitFor(
                        new Locator.WaitForOptions()
                                .setState(com.microsoft.playwright.options.WaitForSelectorState.HIDDEN)
                                .setTimeout(5000)
                );
                System.out.println("Модальное окно закрылось");
            } catch (Exception ignored) {
                System.out.println("Модальное окно уже закрыто или не было открыто");
            }
        } catch (com.microsoft.playwright.TimeoutError e) {
            System.out.println("Кнопка «Выбрать» не найдена, пропускаем этот шаг");
        }
    }
}