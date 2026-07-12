package com.ptn.strategy.news.translation;

import java.util.function.Consumer;

public interface StreamingLlmClient {

    void streamAnswerWithSources(String question, String sourceContext, Consumer<String> tokenConsumer);
}
