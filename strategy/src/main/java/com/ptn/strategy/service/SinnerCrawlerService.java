package com.ptn.strategy.service;

import com.ptn.strategy.util.PlaywrightUtil;
import com.ptn.strategy.entity.SinnerTask;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;
import com.ptn.strategy.mapper.SinnerTaskMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class SinnerCrawlerService {

    // 注入 SinnerTaskMapper 用于管理爬取进度
    private final SinnerTaskMapper sinnerTaskMapper;
    private final SqsTemplate sqsTemplate;

    @Value("${project.sqs.queue-name}")
    private String queueName;

    public SinnerCrawlerService(SinnerTaskMapper sinnerTaskMapper, SqsTemplate sqsTemplate) {
        this.sinnerTaskMapper = sinnerTaskMapper;
        this.sqsTemplate = sqsTemplate;
    }

    /**
     * 模块一：种子任务初始化 (Task Discovery)
     * 作用：访问 Fandom 的角色列表页，获取所有名字并存入 sinner_task 表。
     * 策略：使用 INSERT IGNORE，只增加新发现的角色，不影响已有的任务。新增角色同步到AWS SQS
     */
    public void syncAndDispatchTasks() {
        try{
            // 1. 使用 Playwright 访问角色列表汇总页
            List<String> sinnerNames = new ArrayList<>();
            String url = "https://path-to-nowhere.fandom.com/wiki/Sinners";
            Playwright playwright = Playwright.create();
            BrowserContext context = PlaywrightUtil.createRandomStealthContext(playwright);

            Page page = context.newPage();
            System.out.println("正在访问角色列表页: " + url);
            page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            // 等待表格渲染（确保页面加载到了包含 List of Sinners 的部分）
            page.waitForSelector("h2:has-text('List of Sinners')");

            // 2. 使用 Jsoup 解析出所有角色的英文名
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
            // 3. 循环调用 sinnerTaskMapper.insertTask(name)
            for (String sinnerName : sinnerNames) {
                int result = sinnerTaskMapper.insertTask(sinnerName);

                // 新插入的角色
                if (result > 0) {
                    sqsTemplate.send(to -> to.queue(queueName).payload(sinnerName));
                    System.out.println("任务已派发至 SQS: " + sinnerName);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 只有当配置文件中 scheduler-enabled 为 true 时，才执行定时生产任务
    @Scheduled(cron = "0 0 1 * * ?")
    @ConditionalOnProperty(prefix = "project.crawler", name = "scheduler-enabled", havingValue = "true")
    public void autoSyncTasks() {
        syncAndDispatchTasks();
    }

    /**
     * 检测 sinner_task 中失败的爬取任务，并且尝试重新爬取
     */
    public void retryFailedTasks() {
        try {
            List<SinnerTask> failedTasks = sinnerTaskMapper.findFailedTasks();

            for (SinnerTask failedTask : failedTasks) {
                String sinnerName = failedTask.getNameEn();

                sqsTemplate.send(to -> to.queue(queueName).payload(sinnerName));
                System.out.println("重试任务已派发至 SQS: " + sinnerName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
