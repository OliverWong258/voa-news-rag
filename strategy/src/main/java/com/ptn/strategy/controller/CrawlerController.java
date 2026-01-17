package com.ptn.strategy.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import com.ptn.strategy.service.SinnerCrawlerService;
import com.ptn.strategy.service.BilibiliCrawlerService;
import org.springframework.web.bind.annotation.RequestParam;



@Slf4j // 自动注入 log 对象
@RestController
@RequestMapping("/crawler")
public class CrawlerController {
    private final SinnerCrawlerService sinnerCrawlerService;
    private final BilibiliCrawlerService bilibiliCrawlerService;

    public CrawlerController(SinnerCrawlerService sinnerCrawlerService, BilibiliCrawlerService bilibiliCrawlerService) {
        this.sinnerCrawlerService = sinnerCrawlerService;
        this.bilibiliCrawlerService = bilibiliCrawlerService;
    }

    /**
     * 手动触发同步逻辑：
     * 1. 爬取 Fandom 列表
     * 2. 存入数据库
     * 3. 发送任务到 AWS SQS
     */
    @GetMapping("/sync")
    public ResponseEntity<String> startSync() {
        log.info("=== 收到同步请求，开始派发分布式任务 ===");

        new Thread(sinnerCrawlerService::syncAndDispatchTasks).start();

        return ResponseEntity.ok("同步任务已在后台启动，请观察 Console 监控分布式进度。");
    }
    
    /**
     * 手动触发重试爬取失败的角色
     */
    @GetMapping("/retry")
    public ResponseEntity<String> startRetry() {
        log.info("=== 收到重试请求，开始派发分布式任务 ===");

        new Thread(sinnerCrawlerService::retryFailedTasks).start();

        return ResponseEntity.ok("重试任务已在后台启动，请观察 Console 监控分布式进度。");
    }

    /**
     * 手动触发调用B站API搜索相关专栏
     */
    @GetMapping("/arcId")
    public ResponseEntity<String> startGetArticleIds(@RequestParam("keyword") String keyword) {
        log.info("=== 收到请求，开始调用B站API搜索相关专栏 ===");

        new Thread(() -> {bilibiliCrawlerService.searchArticleIds(keyword);}).start();

        return ResponseEntity.ok("调用任务已在后台启动，请观察 Console 监控分布式进度。");
    }
    
    /**
     * 手动触发爬取专栏并存入AWS S3
     */
    @GetMapping("/rawContent")
    public ResponseEntity<String> saveToS3(@RequestParam("arcId") String arcId) {
        log.info("=== 收到请求，开始爬取专栏内容并存入S3 ===");

        new Thread(() -> {bilibiliCrawlerService.fetchAndSaveToS3(arcId);}).start();

        return ResponseEntity.ok("调用任务已在后台启动，请观察 Console 监控分布式进度。");
    }
}
