package com.ptn.strategy.news.ingestion;

import com.ptn.strategy.news.article.ContentHasher;
import com.ptn.strategy.news.article.NewsArticle;
import com.ptn.strategy.news.article.NewsArticleMapper;
import com.ptn.strategy.news.article.ParsedArticle;
import com.ptn.strategy.news.article.VoaArticleParser;
import com.ptn.strategy.news.task.CrawlTaskMapper;
import com.ptn.strategy.news.task.CrawlTaskMessage;
import com.ptn.strategy.news.task.CrawlTask;
import com.ptn.strategy.news.task.TaskExhaustedException;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.Visibility;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import com.ptn.strategy.config.AwsProperties;
import com.ptn.strategy.news.translation.ArticleProcessMessage;
import io.awspring.cloud.sqs.operations.SqsTemplate;

@Slf4j
@Component
public class VoaCrawlConsumer {

    static final String CONTENT_HASHES_KEY = "voa:ingestion:content-hashes";
    private static final Pattern ARTICLE_ID_PATTERN = Pattern.compile("/(\\d+)\\.html$");

    private final ArticlePageFetcher pageFetcher;
    private final RawHtmlStorage rawHtmlStorage;
    private final VoaArticleParser articleParser;
    private final ContentHasher contentHasher;
    private final StringRedisTemplate redisTemplate;
    private final NewsArticleMapper newsArticleMapper;
    private final CrawlTaskMapper crawlTaskMapper;
    private final AwsProperties awsProperties;
    private final SqsTemplate sqsTemplate;
    private final String workerId = resolveWorkerId();

    public VoaCrawlConsumer(
            ArticlePageFetcher pageFetcher,
            RawHtmlStorage rawHtmlStorage,
            VoaArticleParser articleParser,
            ContentHasher contentHasher,
            StringRedisTemplate redisTemplate,
            NewsArticleMapper newsArticleMapper,
            CrawlTaskMapper crawlTaskMapper,
            AwsProperties awsProperties,
            SqsTemplate sqsTemplate) {
        this.pageFetcher = pageFetcher;
        this.rawHtmlStorage = rawHtmlStorage;
        this.articleParser = articleParser;
        this.contentHasher = contentHasher;
        this.redisTemplate = redisTemplate;
        this.newsArticleMapper = newsArticleMapper;
        this.crawlTaskMapper = crawlTaskMapper;
        this.awsProperties = awsProperties;
        this.sqsTemplate = sqsTemplate;
    }

    @SqsListener(
            queueNames = "${aws.sqs.crawl-queue}",
            messageVisibilitySeconds = "${aws.sqs.visibility-timeout-seconds}")
    public void consume(CrawlTaskMessage message, Visibility visibility) {
        if (crawlTaskMapper.claimForCrawling(message.taskId(), workerId) == 0) {
            CrawlTask existing = crawlTaskMapper.findById(message.taskId());
            if (existing != null && existing.getStatus() == com.ptn.strategy.news.task.TaskStatus.DEAD) {
                throw new TaskExhaustedException(message.taskId());
            }
            log.info("Ignoring duplicate or non-runnable crawl task {}", message.taskId());
            return;
        }

        try {
            CrawlTask task = crawlTaskMapper.findById(message.taskId());
            String rawS3Key = task == null ? null : task.getRawS3Key();
            String html;
            if (rawS3Key == null || rawS3Key.isBlank()) {
                html = pageFetcher.fetch(message.canonicalUrl());
                visibility.changeTo(visibilityTimeoutSeconds());
                rawS3Key = rawHtmlStorage.store(message.taskId(), html);
                crawlTaskMapper.recordRawSnapshot(message.taskId(), rawS3Key);
            } else {
                html = rawHtmlStorage.load(rawS3Key);
                log.info("Replaying crawl task {} from S3 snapshot {}", message.taskId(), rawS3Key);
            }
            visibility.changeTo(visibilityTimeoutSeconds());
            ParsedArticle parsed = articleParser.parse(message.canonicalUrl(), html);
            String contentHash = contentHasher.sha256(parsed.content());

            Long hashAdded = redisTemplate.opsForSet().add(CONTENT_HASHES_KEY, contentHash);
            if (hashAdded == null || hashAdded == 0) {
                NewsArticle existing = newsArticleMapper.findByContentHash(contentHash);
                dispatchForTranslationIfNeeded(existing);
                crawlTaskMapper.markCrawled(message.taskId());
                log.info("Skipped duplicate VOA content for task {}", message.taskId());
                return;
            }

            try {
                NewsArticle article = toNewsArticle(message, parsed, contentHash, rawS3Key);
                int inserted = newsArticleMapper.insertIfAbsent(article);
                if (inserted == 0) {
                    article = newsArticleMapper.findByCanonicalUrl(message.canonicalUrl());
                    if (article == null) {
                        article = newsArticleMapper.findByContentHash(contentHash);
                    }
                }
                if (article == null || article.getId() == null) {
                    throw new IllegalStateException("Unable to resolve persisted article for task " + message.taskId());
                }
                dispatchForTranslationIfNeeded(article);
                crawlTaskMapper.markCrawled(message.taskId());
                log.info("VOA crawl task {} completed; article inserted={}", message.taskId(), inserted == 1);
            } catch (RuntimeException exception) {
                redisTemplate.opsForSet().remove(CONTENT_HASHES_KEY, contentHash);
                throw exception;
            }
        } catch (RuntimeException exception) {
            crawlTaskMapper.markFailed(message.taskId(), errorMessage(exception));
            log.error("VOA crawl task {} failed", message.taskId(), exception);
            throw exception;
        }
    }

    private void dispatchForTranslationIfNeeded(NewsArticle article) {
        if (article == null || article.getId() == null) {
            return;
        }
        String status = article.getProcessingStatus();
        if (status == null || "CRAWLED".equals(status) || "TRANSLATION_FAILED".equals(status)) {
            sqsTemplate.send(to -> to
                    .queue(awsProperties.sqs().processQueue())
                    .payload(new ArticleProcessMessage(article.getId())));
        }
    }

    private int visibilityTimeoutSeconds() {
        return awsProperties.sqs().visibilityTimeoutSeconds();
    }

    private NewsArticle toNewsArticle(
            CrawlTaskMessage message, ParsedArticle parsed, String contentHash, String rawS3Key) {
        NewsArticle article = new NewsArticle();
        article.setSource("VOA");
        article.setSourceArticleId(extractArticleId(message.canonicalUrl()));
        article.setUrl(message.url());
        article.setCanonicalUrl(message.canonicalUrl());
        article.setTitleEn(parsed.title());
        article.setContentEn(parsed.content());
        article.setAuthor(parsed.author());
        article.setPublishedAt(parsed.publishedAt());
        article.setContentHash(contentHash);
        article.setLanguage("en");
        article.setRawS3Key(rawS3Key);
        article.setProcessingStatus("CRAWLED");
        return article;
    }

    private String extractArticleId(String url) {
        Matcher matcher = ARTICLE_ID_PATTERN.matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String errorMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static String resolveWorkerId() {
        String host = System.getenv("HOSTNAME");
        return (host == null || host.isBlank() ? "worker" : host) + "-" + UUID.randomUUID();
    }
}
