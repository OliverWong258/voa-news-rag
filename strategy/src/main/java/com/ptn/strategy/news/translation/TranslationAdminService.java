package com.ptn.strategy.news.translation;

import com.ptn.strategy.config.AwsProperties;
import com.ptn.strategy.news.article.NewsArticleMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.stereotype.Service;
import com.ptn.strategy.news.indexing.ArticleIndexMessage;

@Service
public class TranslationAdminService {

    private final NewsArticleMapper articleMapper;
    private final SqsTemplate sqsTemplate;
    private final AwsProperties awsProperties;

    public TranslationAdminService(
            NewsArticleMapper articleMapper, SqsTemplate sqsTemplate, AwsProperties awsProperties) {
        this.articleMapper = articleMapper;
        this.sqsTemplate = sqsTemplate;
        this.awsProperties = awsProperties;
    }

    public boolean retranslate(long articleId) {
        if (articleMapper.resetTranslation(articleId) == 0) {
            return false;
        }
        try {
            sqsTemplate.send(to -> to
                    .queue(awsProperties.sqs().processQueue())
                    .payload(new ArticleProcessMessage(articleId)));
        } catch (RuntimeException exception) {
            articleMapper.markTranslationDispatchFailed(articleId, exception.getMessage());
            throw exception;
        }
        return true;
    }

    public boolean reindex(long articleId) {
        if (articleMapper.resetIndexing(articleId) == 0) {
            return false;
        }
        try {
            sqsTemplate.send(to -> to
                    .queue(awsProperties.sqs().indexQueue())
                    .payload(new ArticleIndexMessage(articleId)));
            articleMapper.markIndexQueued(articleId);
        } catch (RuntimeException exception) {
            articleMapper.markIndexDispatchFailed(articleId, exception.getMessage());
            throw exception;
        }
        return true;
    }
}
