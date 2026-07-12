package com.ptn.strategy.news.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

class CachingEmbeddingServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    void reusesVectorFromRedisWithoutCallingEmbeddingApi() {
        EmbeddingClient client = mock(EmbeddingClient.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("voa:embedding:hash")).thenReturn("[0.1,0.2,0.3]");
        CachingEmbeddingService service = new CachingEmbeddingService(
                client, redis, new ObjectMapper());

        List<Float> vector = service.embed("hash", "新闻内容");

        assertThat(vector).containsExactly(0.1f, 0.2f, 0.3f);
        verify(client, never()).embed("新闻内容");
    }

    @SuppressWarnings("unchecked")
    @Test
    void cachesNewEmbedding() {
        EmbeddingClient client = mock(EmbeddingClient.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(client.embed("新闻内容")).thenReturn(List.of(0.4f, 0.5f));
        CachingEmbeddingService service = new CachingEmbeddingService(
                client, redis, new ObjectMapper());

        assertThat(service.embed("new-hash", "新闻内容")).containsExactly(0.4f, 0.5f);

        verify(values).set("voa:embedding:new-hash", "[0.4,0.5]");
    }
}
