package com.ptn.strategy.news.rag;

import com.ptn.strategy.config.LlmProperties;
import com.ptn.strategy.news.search.NewsSearchRequest;
import com.ptn.strategy.news.search.NewsSearchResponse;
import com.ptn.strategy.news.search.SearchHit;
import com.ptn.strategy.news.search.SemanticSearchService;
import com.ptn.strategy.news.translation.LlmClient;
import com.ptn.strategy.news.translation.LlmResponseException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

@Service
public class RagService {

    private static final String NO_EVIDENCE_ANSWER = "当前新闻库中没有足够的相关证据来回答这个问题。";

    private final SemanticSearchService searchService;
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;

    public RagService(
            SemanticSearchService searchService,
            LlmClient llmClient,
            LlmProperties llmProperties) {
        this.searchService = searchService;
        this.llmClient = llmClient;
        this.llmProperties = llmProperties;
    }

    public RagAnswerResponse answer(RagQuestionRequest request) {
        NewsSearchResponse search = searchService.search(new NewsSearchRequest(
                request.question(), request.topK(), request.startDate(), request.endDate()));
        if (search.hits().isEmpty()) {
            return new RagAnswerResponse(NO_EVIDENCE_ANSWER, false, List.of());
        }

        Context context = buildContext(search.hits());
        if (context.sources().isEmpty()) {
            return new RagAnswerResponse(NO_EVIDENCE_ANSWER, false, List.of());
        }
        String answer = withRetry(() -> llmClient.answerWithSources(
                request.question().trim(), context.text()));
        return new RagAnswerResponse(answer, true, context.sources());
    }

    private Context buildContext(List<SearchHit> hits) {
        StringBuilder context = new StringBuilder();
        List<RagSource> sources = new ArrayList<>();
        int maxChars = llmProperties.ragMaxContextChars();

        for (SearchHit hit : hits) {
            int citation = sources.size() + 1;
            String excerpt = sanitize(hit.excerpt());
            String blockPrefix = "<source id=\"" + citation + "\">\n标题："
                    + sanitize(hit.title()) + "\nURL：" + sanitize(hit.sourceUrl()) + "\n内容：";
            String blockSuffix = "\n</source>\n\n";
            int available = maxChars - context.length() - blockPrefix.length() - blockSuffix.length();
            if (available <= 0) {
                break;
            }
            if (excerpt.length() > available) {
                excerpt = excerpt.substring(0, available);
            }
            context.append(blockPrefix).append(excerpt).append(blockSuffix);
            sources.add(new RagSource(
                    citation,
                    hit.articleId(),
                    hit.chunkId(),
                    hit.title(),
                    hit.sourceUrl(),
                    hit.publishedAt(),
                    hit.score(),
                    excerpt));
        }
        return new Context(context.toString(), List.copyOf(sources));
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace("</source>", "&lt;/source&gt;");
    }

    private String withRetry(Supplier<String> operation) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= llmProperties.maxAttempts(); attempt++) {
            try {
                return operation.get();
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }
        throw new LlmResponseException(
                "RAG generation failed after " + llmProperties.maxAttempts() + " attempts",
                lastFailure);
    }

    private record Context(String text, List<RagSource> sources) {
    }
}
