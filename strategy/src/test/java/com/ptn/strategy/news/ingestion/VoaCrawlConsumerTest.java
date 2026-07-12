package com.ptn.strategy.news.ingestion;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ptn.strategy.config.AwsProperties;
import com.ptn.strategy.news.article.ContentHasher;
import com.ptn.strategy.news.article.NewsArticleMapper;
import com.ptn.strategy.news.article.ParsedArticle;
import com.ptn.strategy.news.article.VoaArticleParser;
import com.ptn.strategy.news.task.CrawlTask;
import com.ptn.strategy.news.task.CrawlTaskMapper;
import com.ptn.strategy.news.task.CrawlTaskMessage;
import io.awspring.cloud.sqs.listener.Visibility;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class VoaCrawlConsumerTest {

    @SuppressWarnings("unchecked")
    @Test
    void replaysExistingS3SnapshotWithoutRecrawlingVoa() {
        ArticlePageFetcher pageFetcher = mock(ArticlePageFetcher.class);
        RawHtmlStorage rawHtmlStorage = mock(RawHtmlStorage.class);
        VoaArticleParser parser = mock(VoaArticleParser.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        SetOperations<String, String> setOperations = mock(SetOperations.class);
        NewsArticleMapper articleMapper = mock(NewsArticleMapper.class);
        CrawlTaskMapper taskMapper = mock(CrawlTaskMapper.class);
        Visibility visibility = mock(Visibility.class);

        CrawlTask task = new CrawlTask();
        task.setId(42L);
        task.setRawS3Key("raw/voa/tasks/42.html");
        CrawlTaskMessage message = new CrawlTaskMessage(
                42L,
                "https://www.voanews.com/a/story/1234567.html",
                "https://www.voanews.com/a/story/1234567.html");

        when(taskMapper.claimForCrawling(any(Long.class), anyString())).thenReturn(1);
        when(taskMapper.findById(42L)).thenReturn(task);
        when(rawHtmlStorage.load(task.getRawS3Key())).thenReturn("<html>snapshot</html>");
        when(parser.parse(anyString(), anyString())).thenReturn(new ParsedArticle(
                "Title", "A sufficiently complete article body.", "VOA", LocalDateTime.now()));
        when(redis.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), anyString())).thenReturn(1L);
        when(articleMapper.insertIfAbsent(any())).thenReturn(1);

        AwsProperties properties = new AwsProperties(
                "us-east-1",
                new AwsProperties.Sqs("crawl", "process", "crawl-dlq", "process-dlq", 300, 5),
                new AwsProperties.S3("raw-bucket"));
        VoaCrawlConsumer consumer = new VoaCrawlConsumer(
                pageFetcher,
                rawHtmlStorage,
                parser,
                new ContentHasher(),
                redis,
                articleMapper,
                taskMapper,
                properties);

        consumer.consume(message, visibility);

        verify(pageFetcher, never()).fetch(anyString());
        verify(rawHtmlStorage).load("raw/voa/tasks/42.html");
        verify(articleMapper).insertIfAbsent(any());
        verify(taskMapper).markCrawled(42L);
        verify(visibility).changeTo(300);
    }
}
