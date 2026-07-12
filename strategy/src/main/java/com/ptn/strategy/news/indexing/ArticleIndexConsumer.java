package com.ptn.strategy.news.indexing;

import com.ptn.strategy.config.AwsProperties;
import com.ptn.strategy.config.MilvusProperties;
import com.ptn.strategy.news.article.NewsArticle;
import com.ptn.strategy.news.article.NewsArticleMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.Visibility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArticleIndexConsumer {

    private final NewsArticleMapper articleMapper;
    private final ArticleIndexingService indexingService;
    private final MilvusProperties milvusProperties;
    private final AwsProperties awsProperties;

    public ArticleIndexConsumer(
            NewsArticleMapper articleMapper,
            ArticleIndexingService indexingService,
            MilvusProperties milvusProperties,
            AwsProperties awsProperties) {
        this.articleMapper = articleMapper;
        this.indexingService = indexingService;
        this.milvusProperties = milvusProperties;
        this.awsProperties = awsProperties;
    }

    @SqsListener(
            queueNames = "${aws.sqs.index-queue}",
            messageVisibilitySeconds = "${aws.sqs.visibility-timeout-seconds}")
    public void consume(ArticleIndexMessage message, Visibility visibility) {
        if (articleMapper.claimForIndexing(
                message.articleId(), milvusProperties.indexingMaxAttempts()) == 0) {
            NewsArticle existing = articleMapper.findById(message.articleId());
            if (existing != null && "INDEX_DEAD".equals(existing.getProcessingStatus())) {
                throw new IllegalStateException(
                        "Article " + message.articleId() + " exhausted its indexing retry budget");
            }
            log.info("Ignoring duplicate or non-runnable indexing task {}", message.articleId());
            return;
        }

        try {
            NewsArticle article = articleMapper.findById(message.articleId());
            if (article == null) {
                throw new IllegalStateException("Article not found: " + message.articleId());
            }
            int indexedChunks = indexingService.index(
                    article,
                    () -> visibility.changeTo(awsProperties.sqs().visibilityTimeoutSeconds()));
            if (articleMapper.markIndexed(article.getId()) != 1) {
                throw new IllegalStateException("Article indexing state changed concurrently: " + article.getId());
            }
            log.info("Article {} indexed in Milvus; new chunks={}", article.getId(), indexedChunks);
        } catch (RuntimeException exception) {
            articleMapper.markIndexingFailed(
                    message.articleId(), errorMessage(exception), milvusProperties.indexingMaxAttempts());
            log.error("Article {} indexing failed", message.articleId(), exception);
            throw exception;
        }
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
