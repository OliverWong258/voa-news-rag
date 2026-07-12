package com.ptn.strategy.news.ingestion;

import com.ptn.strategy.news.article.ContentHasher;
import com.ptn.strategy.news.article.NewsArticle;
import com.ptn.strategy.news.article.NewsArticleMapper;
import com.ptn.strategy.news.article.ParsedArticle;
import com.ptn.strategy.news.article.VoaArticleParser;
import com.ptn.strategy.news.task.CrawlTaskMapper;
import com.ptn.strategy.news.task.CrawlTaskMessage;
import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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
    private final String workerId = resolveWorkerId();

    public VoaCrawlConsumer(
            ArticlePageFetcher pageFetcher,
            RawHtmlStorage rawHtmlStorage,
            VoaArticleParser articleParser,
            ContentHasher contentHasher,
            StringRedisTemplate redisTemplate,
            NewsArticleMapper newsArticleMapper,
            CrawlTaskMapper crawlTaskMapper) {
        this.pageFetcher = pageFetcher;
        this.rawHtmlStorage = rawHtmlStorage;
        this.articleParser = articleParser;
        this.contentHasher = contentHasher;
        this.redisTemplate = redisTemplate;
        this.newsArticleMapper = newsArticleMapper;
        this.crawlTaskMapper = crawlTaskMapper;
    }

    @SqsListener("${aws.sqs.crawl-queue}")
    public void consume(CrawlTaskMessage message) {
        if (crawlTaskMapper.claimForCrawling(message.taskId(), workerId) == 0) {
            log.info("Ignoring duplicate or non-runnable crawl task {}", message.taskId());
            return;
        }

        try {
            String html = pageFetcher.fetch(message.canonicalUrl());
            String rawS3Key = rawHtmlStorage.store(message.taskId(), html);
            crawlTaskMapper.recordRawSnapshot(message.taskId(), rawS3Key);
            ParsedArticle parsed = articleParser.parse(message.canonicalUrl(), html);
            String contentHash = contentHasher.sha256(parsed.content());

            Long hashAdded = redisTemplate.opsForSet().add(CONTENT_HASHES_KEY, contentHash);
            if (hashAdded == null || hashAdded == 0) {
                crawlTaskMapper.markCrawled(message.taskId());
                log.info("Skipped duplicate VOA content for task {}", message.taskId());
                return;
            }

            try {
                NewsArticle article = toNewsArticle(message, parsed, contentHash, rawS3Key);
                int inserted = newsArticleMapper.insertIfAbsent(article);
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
