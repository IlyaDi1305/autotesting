package ru.russpass.e2e;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

final class PageActions {
    private PageActions() {
    }

    static void acceptCookies(Page page) {
        Locator cookie = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Хорошо"));
        if (cookie.count() > 0 && cookie.first().isVisible()) {
            cookie.click();
        }
    }

    static void typeInto(Page page, String selector, String value) {
        Locator field = page.locator(selector);
        field.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE));
        field.click();
        field.press("ControlOrMeta+A");
        field.press("Backspace");
        // Payecom/Sber валидирует через key events — обычный fill часто не активирует «Оплатить»
        field.pressSequentially(value, new Locator.PressSequentiallyOptions().setDelay(50.0));
        field.blur();
    }

    static void increaseAdultTickets(Page page) {
        page.getByText("Пассажиры", new Page.GetByTextOptions().setExact(true)).click();
        Locator sheet = page.locator("[data-rsbs-content=\"true\"]");
        assertThat(sheet).isVisible(new LocatorAssertions.IsVisibleOptions().setTimeout(10_000));

        boolean increased = Boolean.TRUE.equals(page.evaluate("""
                () => {
                  const root = document.querySelector('[data-rsbs-content="true"]') || document.body;
                  const row = Array.from(root.querySelectorAll('div')).find((el) => {
                    const text = (el.textContent || '').replace(/\\s+/g, ' ').trim();
                    const buttons = el.querySelectorAll('button');
                    return /Взрослый/i.test(text) && buttons.length === 2 && text.length < 120;
                  });
                  if (!row) return false;
                  const buttons = Array.from(row.querySelectorAll('button'));
                  buttons[buttons.length - 1].click();
                  return true;
                }
                """));

        if (!increased) {
            // fallback: Назад + пары (−/+) по тарифам → плюс первого тарифа
            sheet.getByRole(AriaRole.BUTTON).nth(2).click();
        }
    }
}
