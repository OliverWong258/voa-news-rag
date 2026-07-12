package com.ptn.strategy.news.recovery;

import com.ptn.strategy.config.AwsProperties;
import com.ptn.strategy.config.CrawlerProperties;
import com.ptn.strategy.news.task.CrawlTask;
import com.ptn.strategy.news.task.CrawlTaskMapper;
import com.ptn.strategy.news.task.CrawlTaskMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskRecoveryService {

    private final CrawlTaskMapper taskMapper;
    private final SqsTemplate sqsTemplate;
    private final AwsProperties awsProperties;
    private final CrawlerProperties crawlerProperties;

    public TaskRecoveryService(
            CrawlTaskMapper taskMapper,
            SqsTemplate sqsTemplate,
            AwsProperties awsProperties,
            CrawlerProperties crawlerProperties) {
        this.taskMapper = taskMapper;
        this.sqsTemplate = sqsTemplate;
        this.awsProperties = awsProperties;
        this.crawlerProperties = crawlerProperties;
    }

    @Scheduled(cron = "${crawler.recovery-cron}")
    public void recoverStaleTasks() {
        LocalDateTime cutoff = LocalDateTime.now(ZoneOffset.UTC)
                .minus(crawlerProperties.staleTaskThreshold());
        List<CrawlTask> staleTasks = taskMapper.findStaleCrawling(
                cutoff, crawlerProperties.recoveryBatchSize());

        for (CrawlTask task : staleTasks) {
            if (taskMapper.markRetryingIfStale(task.getId(), cutoff) == 1) {
                redispatch(task);
            }
        }
    }

    public boolean replayFailedTask(long taskId) {
        CrawlTask task = taskMapper.findById(taskId);
        if (task == null || taskMapper.markRetrying(taskId) == 0) {
            return false;
        }
        redispatch(task);
        return true;
    }

    private void redispatch(CrawlTask task) {
        try {
            CrawlTaskMessage message = new CrawlTaskMessage(
                    task.getId(), task.getUrl(), task.getCanonicalUrl());
            sqsTemplate.send(to -> to.queue(awsProperties.sqs().crawlQueue()).payload(message));
            log.info("Redispatched crawl task {}", task.getId());
        } catch (RuntimeException exception) {
            taskMapper.markRedispatchFailed(task.getId(), exception.getMessage());
            throw exception;
        }
    }
}
