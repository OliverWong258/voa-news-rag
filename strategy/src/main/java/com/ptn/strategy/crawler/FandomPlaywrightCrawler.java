package com.ptn.strategy.crawler;

import java.util.ArrayList;
import java.util.List;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

public class FandomPlaywrightCrawler {
    public List<String> fetchSinnerList() {
        List<String> sinnerNames = new ArrayList<>();
        // 注意：这里的 URL 是 /wiki/Sinners，即你提到的 List of Sinners 页面
        String url = "https://path-to-nowhere.fandom.com/wiki/Sinners";

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            BrowserContext context = browser.newContext();
            // 保持之前的 Stealth 脚本以绕过验证
            context.addInitScript("() => { Object.defineProperty(navigator, 'webdriver', {get: () => undefined}); }");
            
            Page page = context.newPage();
            System.out.println("正在访问角色列表页: " + url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // 等待表格渲染（确保页面加载到了包含 List of Sinners 的部分）
            page.waitForSelector("h2:has-text('List of Sinners')");

            // 使用 Jsoup 解析内容
            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(page.content());

            // --- 核心解析逻辑 ---
            
            // 1. 尝试找到那个表格
            // 逻辑：找到包含“List of Sinners”文字的 h2，找它后面紧跟的 center 里的 table
            org.jsoup.nodes.Element table = doc.selectFirst("h2:contains(List of Sinners) ~ center table");
            
            if (table != null) {
                // 2. 找到表格中所有的行 (tr)
                org.jsoup.select.Elements rows = table.select("tbody tr");
                
                for (org.jsoup.nodes.Element row : rows) {
                    // 3. 找到每行中的所有单元格 (td)
                    org.jsoup.select.Elements cols = row.select("td");
                    
                    // 4. 根据你的描述，角色名在第二列 (索引为 1)
                    if (cols.size() >= 2) {
                        org.jsoup.nodes.Element nameLink = cols.get(1).selectFirst("a");
                        if (nameLink != null) {
                            // 提取 title 属性的值
                            String nameEn = nameLink.attr("title");
                            sinnerNames.add(nameEn);
                        }
                    }
                }
            }
            else {
                System.out.println("无法定位表格");
            }

            System.out.println("成功抓取到角色数量: " + sinnerNames.size());
            System.out.println("前 5 个角色示例: " + sinnerNames.stream().limit(5).toList());
            
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sinnerNames;
    }

    public static void main(String[] args) {
        FandomPlaywrightCrawler fandomPlaywrightCrawler = new FandomPlaywrightCrawler();
        fandomPlaywrightCrawler.fetchSinnerList();

        try (Playwright playwright = Playwright.create()) {
            // 建议先保持 headless(false) 观察，成功率稳定后再改回 true
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(java.util.List.of("--disable-blink-features=AutomationControlled")));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"));

            // 注入隐身脚本
            context.addInitScript("() => { Object.defineProperty(navigator, 'webdriver', {get: () => undefined}); }");

            Page page = context.newPage();
            String sinnerName = "Hella";
            String url = "https://path-to-nowhere.fandom.com/wiki/" + sinnerName;

            // System.out.println("正在访问: " + url);
            // page.navigate(url);

            try {
                System.out.println("正在发起访问: " + url);
    
                // 关键点：将等待级别降低到 DOMCONTENTLOADED
                page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

                System.out.println("正在监测页面内容，等待挑战自动完成...");

                try {
                    // 最长等待 30 秒，直到页面上出现 .portable-infobox 标签
                    page.waitForSelector(".portable-infobox", new Page.WaitForSelectorOptions().setTimeout(30000));
                    
                    // 此时页面内容基本已经出来了
                    System.out.println("【成功】页面已加载！当前标题: " + page.title());
                    
                    // 尝试直接获取 HTML 内容
                    String html = page.content();
                    // System.out.println("获取到 HTML 长度: " + htmlSnippet.length());
                    // System.out.println("HTML 前 200 位: " + htmlSnippet.substring(0, 200));

                    // 使用 Jsoup 解析这段 HTML
                    org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);

                    // 提取 Assessment (稀有度)
                    // 逻辑：找到 data-source="Assessment" 的 td，再找它下面的 a 标签，取 title 属性
                    org.jsoup.nodes.Element assessmentElement = doc.selectFirst("td[data-source='Assessment'] a");
                    String assessment = (assessmentElement != null) ? assessmentElement.attr("title") : "未找到";
                    System.out.println("提取到的 Assessment: " + assessment);

                    // 提取 Roles (职业)
                    // 逻辑与 Assessment 相同
                    org.jsoup.nodes.Element rolesElement = doc.selectFirst("td[data-source='Roles'] a");
                    String roles = (rolesElement != null) ? rolesElement.attr("title") : "未找到";
                    System.out.println("提取到的 Roles: " + roles);

                    // 提取 Image URL
                    org.jsoup.nodes.Element imgElement = doc.selectFirst("figure[data-source='Image'] a");
                    String imgURL = (imgElement != null) ? imgElement.attr("href") : "未找到";
                    System.out.println("提取到的 Image URL: " + imgURL);

                    // 提取 Description
                    org.jsoup.nodes.Element descElement = doc.selectFirst("meta[name='description']");
                    String description = (descElement != null) ? descElement.attr("content") : "未找到";
                    System.out.println("提取到的 Description: " + description);

                    // 1. 定位包含 "Criminal Record" 字样的单元格
                    // :contains 是 Jsoup 非常好用的选择器，用于模糊匹配文本
                    org.jsoup.nodes.Element headerCell = doc.selectFirst("td:contains(Performance in Serving Term)");

                    if (headerCell != null) {
                        // 2. 找到这个单元格所属的那个 table
                        org.jsoup.nodes.Element table = headerCell.closest("table");
                        
                        // 3. 获取该 table 下所有的行 (tr)
                        org.jsoup.select.Elements rows = table.select("tr");
                        
                        StringBuilder criminalRecordBuilder = new StringBuilder();
                        
                        // 4. 遍历行。注意：第一行 (index 0) 是标题 "Criminal Record"，我们要从第二行开始
                        for (int i = 1; i < rows.size(); i++) {
                            String rowText = rows.get(i).text();
                            if (!rowText.isEmpty()) {
                                criminalRecordBuilder.append(rowText).append("\n\n");
                            }
                        }
                        
                        String finalRecord = criminalRecordBuilder.toString().trim();
                        System.out.println("提取到的 Criminal Record:\n" + finalRecord);
                    }
                } catch (com.microsoft.playwright.TimeoutError te) {
                    System.err.println("【失败】在验证页停留过久或未发现角色信息框。当前标题: " + page.title());
                }
            } catch (Exception e) {
                System.err.println("解析失败，当前 URL: " + page.url());
            }

            Thread.sleep(3000); 
            browser.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
