package com.ptn.strategy.news.rag;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/qa")
public class RagController {

    private final RagService ragService;
    private final StreamingRagService streamingRagService;

    public RagController(RagService ragService, StreamingRagService streamingRagService) {
        this.ragService = ragService;
        this.streamingRagService = streamingRagService;
    }

    @PostMapping
    public RagAnswerResponse answer(@Valid @RequestBody RagQuestionRequest request) {
        return ragService.answer(request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamAnswer(@Valid @RequestBody RagQuestionRequest request) {
        return streamingRagService.answer(request);
    }
}
