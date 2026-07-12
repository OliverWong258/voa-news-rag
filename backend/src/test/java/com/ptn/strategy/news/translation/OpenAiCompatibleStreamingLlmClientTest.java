package com.ptn.strategy.news.translation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptn.strategy.config.LlmProperties;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleStreamingLlmClientTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void forwardsContentDeltasAsTheyArrive() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(requestBody).contains("\"stream\":true", "发生了什么？", "source text");

            byte[] response = ("data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{\"content\":\"，世界\"}}]}\n\n"
                    + "data: [DONE]\n\n").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();

        LlmProperties properties = new LlmProperties(
                "http://localhost:" + server.getAddress().getPort(),
                "key", "chat-model", "embedding-model",
                Duration.ofSeconds(5), 2, 100, 200, 2000);
        OpenAiCompatibleStreamingLlmClient client =
                new OpenAiCompatibleStreamingLlmClient(properties, new ObjectMapper());
        List<String> tokens = new ArrayList<>();

        client.streamAnswerWithSources("发生了什么？", "source text", tokens::add);

        assertThat(tokens).containsExactly("你好", "，世界");
    }
}
