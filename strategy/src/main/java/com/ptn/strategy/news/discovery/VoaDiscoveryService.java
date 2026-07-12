package com.ptn.strategy.news.discovery;

import com.ptn.strategy.config.AwsProperties;
import com.ptn.strategy.config.CrawlerProperties;
import com.ptn.strategy.news.task.CrawlTask;
import com.ptn.strategy.news.task.CrawlTaskMapper;
import com.ptn.strategy.news.task.CrawlTaskMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.LinkedHashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VoaDiscoveryService {

    static final String DISCOVERED_URLS_KEY = "voa:discovery:article-urls";

    private final DiscoveryPageFetcher pageFetcher;
    private final VoaListingParser listingParser;
    private final CrawlTaskMapper crawlTaskMapper;
    private final StringRedisTemplate redisTemplate;
    private final SqsTemplate sqsTemplate;
    private final CrawlerProperties crawlerProperties;
    private final AwsProperties awsProperties;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public VoaDiscoveryService(
            DiscoveryPageFetcher pageFetcher,
            VoaListingParser listingParser,
            CrawlTaskMapper crawlTaskMapper,
            StringRedisTemplate redisTemplate,
            SqsTemplate sqsTemplate,
            CrawlerProperties crawlerProperties,
            AwsProperties awsProperties) {
        this.pageFetcher = pageFetcher;
        this.listingParser = listingParser;
        this.crawlTaskMapper = crawlTaskMapper;
        this.redisTemplate = redisTemplate;
        this.sqsTemplate = sqsTemplate;
        this.crawlerProperties = crawlerProperties;
        this.awsProperties = awsProperties;
    }

    public DiscoveryResult discover() {
        if (!running.compareAndSet(false, true)) {
            throw new IllegalStateException("A VOA discovery run is already in progress");
        }

        try {
            return runDiscovery();
        } finally {
            running.set(false);
        }
    }

    @Scheduled(cron = "${crawler.discovery-cron}")
    public void scheduledDiscovery() {
        if (!crawlerProperties.schedulerEnabled()) {
            return;
        }
        try {
            DiscoveryResult result = discover();
            log.info("Scheduled VOA discovery completed: {}", result);
        } catch (Exception exception) {
            log.error("Scheduled VOA discovery failed", exception);
        }
    }

    private DiscoveryResult runDiscovery() {
        LinkedHashSet<String> discoveredUrls = new LinkedHashSet<>();
        int pagesVisited = 0;
        int pagesFailed = 0;

        for (String seedUrl : crawlerProperties.seedUrls()) {
            try {
                String html = pageFetcher.fetch(seedUrl);
                discoveredUrls.addAll(listingParser.extractArticleUrls(seedUrl, html));
                pagesVisited++;
            } catch (Exception exception) {
                pagesFailed++;
                log.warn("Unable to discover VOA articles from {}", seedUrl, exception);
            }
        }

        int linksFound = discoveredUrls.size();
        int redisDuplicates = 0;
        int databaseDuplicates = 0;
        int tasksDispatched = 0;
        int examined = 0;

        for (String canonicalUrl : discoveredUrls) {
            if (examined++ >= crawlerProperties.maxLinksPerRun()) {
                break;
            }

            Long added = redisTemplate.opsForSet().add(DISCOVERED_URLS_KEY, canonicalUrl);
            if (added == null || added == 0) {
                redisDuplicates++;
                continue;
            }

            CrawlTask task = new CrawlTask();
            task.setUrl(canonicalUrl);
            task.setCanonicalUrl(canonicalUrl);
            task.setMaxRetries(awsProperties.sqs().maxReceiveCount());

            try {
                if (crawlTaskMapper.insertIfAbsent(task) == 0) {
                    databaseDuplicates++;
                    continue;
                }

                CrawlTaskMessage message = new CrawlTaskMessage(task.getId(), task.getUrl(), task.getCanonicalUrl());
                sqsTemplate.send(to -> to.queue(awsProperties.sqs().crawlQueue()).payload(message));
                crawlTaskMapper.markDispatched(task.getId());
                tasksDispatched++;
            } catch (RuntimeException exception) {
                redisTemplate.opsForSet().remove(DISCOVERED_URLS_KEY, canonicalUrl);
                throw exception;
            }
        }

        return new DiscoveryResult(
                pagesVisited, linksFound, redisDuplicates, databaseDuplicates, tasksDispatched, pagesFailed);
    }
}
