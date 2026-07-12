package com.ptn.strategy.news;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ptn.strategy.news.article.ArticleController;
import com.ptn.strategy.news.article.ArticlePageResponse;
import com.ptn.strategy.news.article.ArticlePreviewResponse;
import com.ptn.strategy.news.article.ArticleQueryService;
import com.ptn.strategy.news.rag.RagAnswerResponse;
import com.ptn.strategy.news.rag.RagController;
import com.ptn.strategy.news.rag.RagQuestionRequest;
import com.ptn.strategy.news.rag.RagService;
import com.ptn.strategy.news.rag.StreamingRagService;
import com.ptn.strategy.news.search.NewsSearchRequest;
import com.ptn.strategy.news.search.NewsSearchResponse;
import com.ptn.strategy.news.search.SearchController;
import com.ptn.strategy.news.search.SemanticSearchService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@WebMvcTest({ArticleController.class, SearchController.class, RagController.class})
class FrontendApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArticleQueryService articleQueryService;
    @MockitoBean
    private SemanticSearchService searchService;
    @MockitoBean
    private RagService ragService;
    @MockitoBean
    private StreamingRagService streamingRagService;

    @Test
    void returnsArticlePreviewPageContract() throws Exception {
        when(articleQueryService.list("Politics", null, null, 0, 20)).thenReturn(
                new ArticlePageResponse(List.of(new ArticlePreviewResponse(
                        7L, "标题", "摘要", "Politics", "VOA", "https://example.com/7",
                        LocalDateTime.parse("2026-07-11T12:00:00"))), 0, 20, 1));

        mockMvc.perform(get("/api/articles").param("category", "Politics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].category").value("Politics"))
                .andExpect(jsonPath("$.items[0].summary").value("摘要"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void passesSearchFiltersToSemanticSearch() throws Exception {
        when(searchService.search(any())).thenReturn(new NewsSearchResponse("query", 0, List.of()));

        mockMvc.perform(get("/api/search")
                        .param("q", "query")
                        .param("category", "Politics")
                        .param("startDate", "2026-07-01")
                        .param("endDate", "2026-07-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("query"));

        ArgumentCaptor<NewsSearchRequest> requestCaptor =
                ArgumentCaptor.forClass(NewsSearchRequest.class);
        verify(searchService).search(requestCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().category())
                .isEqualTo("Politics");
    }

    @Test
    void acceptsCategoryInSynchronousRagContract() throws Exception {
        when(ragService.answer(any())).thenReturn(new RagAnswerResponse("回答 [1]", true, List.of()));

        mockMvc.perform(post("/api/qa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question":"发生了什么？","topK":5,"category":"Politics"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("回答 [1]"))
                .andExpect(jsonPath("$.grounded").value(true));

        ArgumentCaptor<RagQuestionRequest> requestCaptor =
                ArgumentCaptor.forClass(RagQuestionRequest.class);
        verify(ragService).answer(requestCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(requestCaptor.getValue().category())
                .isEqualTo("Politics");
    }

    @Test
    void exposesStreamingRagAsPostEventStream() throws Exception {
        SseEmitter emitter = new SseEmitter();
        emitter.send(SseEmitter.event().name("sources").data("{\"sources\":[]}"));
        emitter.send(SseEmitter.event().name("token").data("{\"text\":\"回答\"}"));
        emitter.send(SseEmitter.event().name("completed").data("{\"grounded\":false}"));
        emitter.complete();
        when(streamingRagService.answer(any())).thenReturn(emitter);

        var result = mockMvc.perform(post("/api/qa/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content("{\"question\":\"未知问题\",\"category\":\"Politics\"}"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:sources")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:token")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("event:completed")));
    }
}
