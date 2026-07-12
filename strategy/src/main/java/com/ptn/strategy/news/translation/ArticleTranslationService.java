package com.ptn.strategy.news.translation;

import com.ptn.strategy.config.LlmProperties;
import com.ptn.strategy.news.article.NewsArticle;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ArticleTranslationService {

    private final LlmClient llmClient;
    private final TranslationChunker chunker;
    private final LlmProperties properties;

    public ArticleTranslationService(
            LlmClient llmClient, TranslationChunker chunker, LlmProperties properties) {
        this.llmClient = llmClient;
        this.chunker = chunker;
        this.properties = properties;
    }

    public ArticleTranslationResult translate(NewsArticle article) {
        return translate(article, () -> { });
    }

    public ArticleTranslationResult translate(NewsArticle article, Runnable heartbeat) {
        if (article.getTitleEn() == null || article.getContentEn() == null) {
            throw new IllegalArgumentException("Article title and English content are required");
        }

        String titleZh = withRetry(() -> llmClient.translateToChinese(article.getTitleEn()));
        heartbeat.run();
        List<String> sourceChunks = chunker.chunk(
                article.getContentEn(), properties.translationChunkChars());
        if (sourceChunks.isEmpty()) {
            throw new IllegalArgumentException("Article English content is empty");
        }

        String contentZh = sourceChunks.stream()
                .map(chunk -> {
                    String translated = withRetry(() -> llmClient.translateToChinese(chunk));
                    heartbeat.run();
                    return translated;
                })
                .collect(Collectors.joining("\n\n"));
        String summarySource = contentZh.substring(
                0, Math.min(contentZh.length(), properties.summarySourceChars()));
        String summaryZh = withRetry(() -> llmClient.summarizeInChinese(summarySource));
        heartbeat.run();
        return new ArticleTranslationResult(titleZh, contentZh, summaryZh);
    }

    private String withRetry(Supplier<String> operation) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= properties.maxAttempts(); attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw new LlmResponseException(
                "LLM operation failed after " + properties.maxAttempts() + " attempts", lastFailure);
    }
}
