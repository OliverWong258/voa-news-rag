package com.ptn.strategy.news.indexing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CachingEmbeddingService {

    private static final String CACHE_PREFIX = "voa:embedding:";
    private static final TypeReference<List<Float>> FLOAT_LIST = new TypeReference<>() { };

    private final EmbeddingClient embeddingClient;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public CachingEmbeddingService(
            EmbeddingClient embeddingClient,
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.embeddingClient = embeddingClient;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Float> embed(String contentHash, String text) {
        String key = CACHE_PREFIX + contentHash;
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, FLOAT_LIST);
            } catch (Exception ignored) {
                redisTemplate.delete(key);
            }
        }

        List<Float> vector = embeddingClient.embed(text);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vector));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to cache embedding", exception);
        }
        return vector;
    }
}
