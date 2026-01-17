package com.ptn.strategy.consumer;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.ptn.strategy.entity.SinnerInfo;
import com.ptn.strategy.mapper.SinnerInfoMapper;
import com.ptn.strategy.mapper.SinnerTaskMapper;
import com.ptn.strategy.util.PlaywrightUtil;

import io.awspring.cloud.sqs.annotation.SqsListener;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

@Component
public class SinnerTaskConsumer {

    // 更新角色爬取状态
    private final SinnerTaskMapper sinnerTaskMapper;
    // 填写角色信息
    private final SinnerInfoMapper sinnerInfoMapper;
    
    public SinnerTaskConsumer(SinnerInfoMapper sinnerInfoMapper, SinnerTaskMapper sinnerTaskMapper) {
        this.sinnerTaskMapper = sinnerTaskMapper;
        this.sinnerInfoMapper = sinnerInfoMapper;
    }

    /**
     * 监听SQS，根据角色名爬取信息
     */
    @SqsListener("${project.sqs.queue-name}")
    public void onMessage(String sinnerName) {
        System.out.println("收到 SQS 任务，准备爬取: " + sinnerName);

        // 使用 PlaywrightUtil 开启环境
        try (Playwright playwright = Playwright.create()) {
            BrowserContext context = PlaywrightUtil.createRandomStealthContext(playwright);
            Page page = context.newPage();
            String url = "https://path-to-nowhere.fandom.com/wiki/" + sinnerName;

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

                // 使用 Jsoup 解析这段 HTML
                org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);

                // 提取 Assessment (稀有度)
                // 逻辑：找到 data-source="Assessment" 的 td，再找它下面的 a 标签，取 title 属性
                org.jsoup.nodes.Element assessmentElement = doc.selectFirst("td[data-source='Assessment'] a");
                String assessment = (assessmentElement != null) ? assessmentElement.attr("title") : "未找到";
                // System.out.println("提取到的 Assessment: " + assessment);

                // 提取 Roles (职业)
                // 逻辑与 Assessment 相同
                org.jsoup.nodes.Element rolesElement = doc.selectFirst("td[data-source='Roles'] a");
                String roles = (rolesElement != null) ? rolesElement.attr("title") : "未找到";
                // System.out.println("提取到的 Roles: " + roles);

                // 提取 Image URL
                org.jsoup.nodes.Element imgElement = doc.selectFirst("figure[data-source='Image'] a");
                String imgURL = (imgElement != null) ? imgElement.attr("href") : "未找到";
                // System.out.println("提取到的 Image URL: " + imgURL);

                // 提取 Description
                org.jsoup.nodes.Element descElement = doc.selectFirst("meta[name='description']");
                String description = (descElement != null) ? descElement.attr("content") : "未找到";
                // System.out.println("提取到的 Description: " + description);

                // 1. 定位包含 "Criminal Record" 字样的单元格
                // :contains 是 Jsoup 非常好用的选择器，用于模糊匹配文本
                org.jsoup.nodes.Element headerCell = doc.selectFirst("td:contains(Criminal Record)");

                String finalRecord = "";

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
                    
                    finalRecord = criminalRecordBuilder.toString().trim();
                    // System.out.println("提取到的 Criminal Record:\n" + finalRecord);
                }

                // 保存到 sinner_info 表
                SinnerInfo sinnerInfo = new SinnerInfo();
                sinnerInfo.setNameEn(sinnerName);
                sinnerInfo.setAssessment(assessment);
                sinnerInfo.setRoles(roles);
                sinnerInfo.setImageUrl(imgURL);
                sinnerInfo.setDescription(description);
                sinnerInfo.setCriminalRecord(finalRecord);
                sinnerInfo.setPerformanceInServingTeam(null); // 暂时不插入相关内容
                sinnerInfo.setCreatedAt(LocalDateTime.now());
                sinnerInfoMapper.insert(sinnerInfo);

                // 更新任务状态为 1 (成功)
                sinnerTaskMapper.updateStatus(sinnerName, 1);
                System.out.println("爬取成功并已更新状态: " + sinnerName);
            } catch (Exception e) {
                // 失败处理：更新状态为 2，并增加重试计数
                sinnerTaskMapper.updateStatus(sinnerName, 2);
                sinnerTaskMapper.incrementRetry(sinnerName);
                System.err.println("爬取失败: " + sinnerName + ", 原因: " + e.getMessage());
            } finally {
                Thread.sleep(3000); 
                context.browser().close();
            }
        } catch (Exception e) {
            System.err.println("解析失败: " + e.getStackTrace());
        }
    }
}
