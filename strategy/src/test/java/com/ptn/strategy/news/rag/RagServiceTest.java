package com.ptn.strategy.news.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ptn.strategy.config.LlmProperties;
import com.ptn.strategy.news.search.NewsSearchRequest;
import com.ptn.strategy.news.search.NewsSearchResponse;
import com.ptn.strategy.news.search.SearchHit;
import com.ptn.strategy.news.search.SemanticSearchService;
import com.ptn.strategy.news.translation.LlmClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RagServiceTest {

    private final LlmProperties properties = new LlmProperties(
            "http://localhost", "key", "chat", "embed",
            Duration.ofSeconds(5), 3, 100, 200, 2000);

    @Test
    void returnsGroundedAnswerAndNumberedSources() {
        SemanticSearchService search = mock(SemanticSearchService.class);
        LlmClient llm = mock(LlmClient.class);
        SearchHit hit = new SearchHit(
                "chunk-1", 10, 20, 0, 0.88, "新闻标题", "新闻证据片段", "Politics",
                "https://www.voanews.com/a/story/1.html", Instant.parse("2026-07-10T00:00:00Z"));
        when(search.search(new NewsSearchRequest("发生了什么？", 5, "Politics", null, null)))
                .thenReturn(new NewsSearchResponse("发生了什么？", 1, List.of(hit)));
        when(llm.answerWithSources(eq("发生了什么？"), contains("新闻证据片段")))
                .thenReturn("根据报道，事件已经发生。[1]");

        RagAnswerResponse response = new RagService(search, llm, properties)
                .answer(new RagQuestionRequest("发生了什么？", 5, "Politics", null, null));

        assertThat(response.grounded()).isTrue();
        assertThat(response.answer()).contains("[1]");
        assertThat(response.sources()).singleElement().satisfies(source -> {
            assertThat(source.citation()).isEqualTo(1);
            assertThat(source.category()).isEqualTo("Politics");
        });
    }

    @Test
    void refusesWithoutCallingLlmWhenThereIsNoEvidence() {
        SemanticSearchService search = mock(SemanticSearchService.class);
        LlmClient llm = mock(LlmClient.class);
        when(search.search(new NewsSearchRequest("未知问题", null, null, null, null)))
                .thenReturn(new NewsSearchResponse("未知问题", 0, List.of()));

        RagAnswerResponse response = new RagService(search, llm, properties)
                .answer(new RagQuestionRequest("未知问题", null, null, null, null));

        assertThat(response.grounded()).isFalse();
        assertThat(response.sources()).isEmpty();
        verify(llm, never()).answerWithSources(eq("未知问题"), contains("source"));
    }
}
