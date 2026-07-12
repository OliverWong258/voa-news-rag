package com.ptn.strategy.news.rag;

import com.ptn.strategy.news.translation.StreamingLlmClient;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class StreamingRagService {

    private final RagService ragService;
    private final StreamingLlmClient llmClient;
    private final Executor executor;

    public StreamingRagService(
            RagService ragService,
            StreamingLlmClient llmClient,
            @Qualifier("ragStreamExecutor") Executor executor) {
        this.ragService = ragService;
        this.llmClient = llmClient;
        this.executor = executor;
    }

    public SseEmitter answer(RagQuestionRequest request) {
        SseEmitter emitter = new SseEmitter(0L);
        executor.execute(() -> stream(request, emitter));
        return emitter;
    }

    private void stream(RagQuestionRequest request, SseEmitter emitter) {
        try {
            RagService.PreparedRag prepared = ragService.prepare(request);
            send(emitter, "sources", Map.of("sources", prepared.sources()));
            if (prepared.sources().isEmpty()) {
                send(emitter, "token", Map.of("text", RagService.NO_EVIDENCE_ANSWER));
                send(emitter, "completed", Map.of("grounded", false));
                emitter.complete();
                return;
            }

            llmClient.streamAnswerWithSources(
                    prepared.question(),
                    prepared.context(),
                    token -> sendUnchecked(emitter, "token", Map.of("text", token)));
            send(emitter, "completed", Map.of("grounded", true));
            emitter.complete();
        } catch (Exception exception) {
            emitter.completeWithError(exception);
        }
    }

    private void sendUnchecked(SseEmitter emitter, String event, Object data) {
        try {
            send(emitter, event, data);
        } catch (IOException exception) {
            throw new SseWriteException(exception);
        }
    }

    private void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }

    private static final class SseWriteException extends RuntimeException {
        private SseWriteException(IOException cause) {
            super(cause);
        }
    }
}
