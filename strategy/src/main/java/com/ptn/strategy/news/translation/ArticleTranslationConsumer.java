package com.ptn.strategy.news.translation;

import com.ptn.strategy.config.AwsProperties;
import com.ptn.strategy.config.LlmProperties;
import com.ptn.strategy.news.article.NewsArticle;
import com.ptn.strategy.news.article.NewsArticleMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.Visibility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ArticleTranslationConsumer {

    private final NewsArticleMapper articleMapper;
    private final ArticleTranslationService translationService;
    private final LlmProperties llmProperties;
    private final AwsProperties awsProperties;

    public ArticleTranslationConsumer(
            NewsArticleMapper articleMapper,
            ArticleTranslationService translationService,
            LlmProperties llmProperties,
            AwsProperties awsProperties) {
        this.articleMapper = articleMapper;
        this.translationService = translationService;
        this.llmProperties = llmProperties;
        this.awsProperties = awsProperties;
    }

    @SqsListener(
            queueNames = "${aws.sqs.process-queue}",
            messageVisibilitySeconds = "${aws.sqs.visibility-timeout-seconds}")
    public void consume(ArticleProcessMessage message, Visibility visibility) {
        if (articleMapper.claimForTranslation(message.articleId(), llmProperties.maxAttempts()) == 0) {
            NewsArticle existing = articleMapper.findById(message.articleId());
            if (existing != null && "TRANSLATION_DEAD".equals(existing.getProcessingStatus())) {
                throw new LlmResponseException(
                        "Article " + message.articleId() + " exhausted its translation retry budget");
            }
            log.info("Ignoring duplicate or non-runnable translation task {}", message.articleId());
            return;
        }

        try {
            NewsArticle article = articleMapper.findById(message.articleId());
            if (article == null) {
                throw new IllegalStateException("Article not found: " + message.articleId());
            }
            ArticleTranslationResult result = translationService.translate(
                    article,
                    () -> visibility.changeTo(awsProperties.sqs().visibilityTimeoutSeconds()));
            int updated = articleMapper.markTranslated(
                    article.getId(),
                    result.titleZh(),
                    result.contentZh(),
                    result.summaryZh(),
                    llmProperties.chatModel());
            if (updated != 1) {
                throw new IllegalStateException("Article translation state changed concurrently: " + article.getId());
            }
            log.info("Article {} translated with model {}", article.getId(), llmProperties.chatModel());
        } catch (RuntimeException exception) {
            articleMapper.markTranslationFailed(
                    message.articleId(), errorMessage(exception), llmProperties.maxAttempts());
            log.error("Article {} translation failed", message.articleId(), exception);
            throw exception;
        }
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
