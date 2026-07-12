package com.ptn.strategy.news.discovery;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitUntilState;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PlaywrightDiscoveryPageFetcher implements DiscoveryPageFetcher {

    @Override
    public String fetch(String url) {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--disable-blink-features=AutomationControlled")));
            Page page = browser.newPage();
            page.navigate(url, new Page.NavigateOptions()
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                    .setTimeout(30_000));
            page.waitForSelector("a[href]", new Page.WaitForSelectorOptions().setTimeout(15_000));
            return page.content();
        }
    }
}
