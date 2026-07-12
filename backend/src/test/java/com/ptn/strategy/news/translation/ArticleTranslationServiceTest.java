package com.ptn.strategy.news.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ptn.strategy.config.LlmProperties;
import com.ptn.strategy.news.article.NewsArticle;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ArticleTranslationServiceTest {

    @Test
    void translatesTitleChunksAndSummary() {
        LlmClient client = mock(LlmClient.class);
        LlmProperties properties = new LlmProperties(
                "http://localhost:11434", "test", "chat", "embedding",
                Duration.ofSeconds(5), 3, 20, 100, 1000);
        ArticleTranslationService service = new ArticleTranslationService(
                client, new TranslationChunker(), properties);
        NewsArticle article = new NewsArticle();
        article.setTitleEn("English title");
        article.setContentEn("First paragraph.\n\nSecond paragraph.");

        when(client.translateToChinese("English title")).thenReturn("中文标题");
        when(client.translateToChinese("First paragraph.")).thenReturn("第一段。");
        when(client.translateToChinese("Second paragraph.")).thenReturn("第二段。");
        when(client.summarizeInChinese("第一段。\n\n第二段。")).thenReturn("中文摘要");

        ArticleTranslationResult result = service.translate(article);

        assertThat(result.titleZh()).isEqualTo("中文标题");
        assertThat(result.contentZh()).isEqualTo("第一段。\n\n第二段。");
        assertThat(result.summaryZh()).isEqualTo("中文摘要");
    }
}
