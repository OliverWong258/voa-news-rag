package com.ptn.strategy.util;

import com.microsoft.playwright.*;
import java.util.List;
import java.util.Random;

public class PlaywrightUtil {
    
    // 预设一组常用的浏览器 User-Agent 列表
    private static final List<String> USER_AGENTS = List.of(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0"
    );

    private static final Random RANDOM = new Random();

    public static BrowserContext createRandomStealthContext(Playwright playwright) {
        // 1. 启动浏览器（可以进一步随机化启动参数）
        Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true) 
                .setArgs(List.of("--disable-blink-features=AutomationControlled")));

        // 2. 随机选择一个 User-Agent
        String randomUserAgent = USER_AGENTS.get(RANDOM.nextInt(USER_AGENTS.size()));

        // 3. 配置上下文（可以随机化 ViewportSize 模拟不同屏幕分辨率）
        BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                .setUserAgent(randomUserAgent)
                .setViewportSize(1920, 1080)); 

        // 4. 注入隐身脚本（保持不变）
        context.addInitScript("() => { Object.defineProperty(navigator, 'webdriver', {get: () => undefined}); }");
        
        return context;
    }
}
