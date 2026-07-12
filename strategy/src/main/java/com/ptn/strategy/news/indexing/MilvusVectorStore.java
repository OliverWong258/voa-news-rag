package com.ptn.strategy.news.indexing;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ptn.strategy.config.MilvusProperties;
import io.milvus.v2.client.ConnectConfig;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.DataType;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.vector.request.UpsertReq;
import jakarta.annotation.PreDestroy;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;

@Component
public class MilvusVectorStore implements VectorStore {

    private final MilvusProperties properties;
    private final Gson gson = new Gson();
    private final AtomicBoolean collectionReady = new AtomicBoolean(false);
    private volatile MilvusClientV2 client;

    public MilvusVectorStore(MilvusProperties properties) {
        this.properties = properties;
    }

    @Override
    public void upsert(VectorDocument document) {
        if (document.embedding().size() != properties.embeddingDimension()) {
            throw new IllegalArgumentException(
                    "Embedding dimension " + document.embedding().size()
                            + " does not match configured dimension " + properties.embeddingDimension());
        }
        ensureCollection();

        JsonObject entity = new JsonObject();
        entity.addProperty("vector_id", document.vectorId());
        entity.addProperty("article_id", document.articleId());
        entity.addProperty("chunk_id", document.chunkId());
        entity.addProperty("chunk_index", document.chunkIndex());
        entity.addProperty("title_zh", truncate(document.titleZh(), 4096));
        entity.addProperty("content_zh", truncate(document.contentZh(), 8192));
        entity.addProperty("source_url", truncate(document.sourceUrl(), 4096));
        entity.addProperty("published_at", document.publishedAt() == null
                ? 0L
                : document.publishedAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        entity.addProperty("content_hash", document.contentHash());
        entity.add("embedding", gson.toJsonTree(document.embedding()));

        client().upsert(UpsertReq.builder()
                .collectionName(properties.collection())
                .data(List.of(entity))
                .build());
    }

    private synchronized void ensureCollection() {
        if (collectionReady.get()) {
            return;
        }
        MilvusClientV2 milvus = client();
        boolean exists = milvus.hasCollection(HasCollectionReq.builder()
                .collectionName(properties.collection())
                .build());
        if (!exists) {
            CreateCollectionReq.CollectionSchema schema = milvus.createSchema();
            schema.addField(field("vector_id", DataType.VarChar, true, 128, null));
            schema.addField(field("article_id", DataType.Int64, false, null, null));
            schema.addField(field("chunk_id", DataType.Int64, false, null, null));
            schema.addField(field("chunk_index", DataType.Int64, false, null, null));
            schema.addField(field("title_zh", DataType.VarChar, false, 4096, null));
            schema.addField(field("content_zh", DataType.VarChar, false, 8192, null));
            schema.addField(field("source_url", DataType.VarChar, false, 4096, null));
            schema.addField(field("published_at", DataType.Int64, false, null, null));
            schema.addField(field("content_hash", DataType.VarChar, false, 64, null));
            schema.addField(field(
                    "embedding", DataType.FloatVector, false, null, properties.embeddingDimension()));

            IndexParam index = IndexParam.builder()
                    .fieldName("embedding")
                    .indexName("embedding_auto_index")
                    .indexType(IndexParam.IndexType.AUTOINDEX)
                    .metricType(IndexParam.MetricType.COSINE)
                    .build();
            milvus.createCollection(CreateCollectionReq.builder()
                    .collectionName(properties.collection())
                    .description("Chinese VOA article chunks for RAG")
                    .collectionSchema(schema)
                    .indexParams(List.of(index))
                    .build());
        }
        collectionReady.set(true);
    }

    private AddFieldReq field(
            String name, DataType type, boolean primary, Integer maxLength, Integer dimension) {
        var builder = AddFieldReq.builder()
                .fieldName(name)
                .dataType(type)
                .isPrimaryKey(primary)
                .autoID(false);
        if (maxLength != null) {
            builder.maxLength(maxLength);
        }
        if (dimension != null) {
            builder.dimension(dimension);
        }
        return builder.build();
    }

    private MilvusClientV2 client() {
        MilvusClientV2 current = client;
        if (current == null) {
            synchronized (this) {
                current = client;
                if (current == null) {
                    ConnectConfig.ConnectConfigBuilder builder = ConnectConfig.builder()
                            .uri(properties.uri());
                    if (properties.token() != null && !properties.token().isBlank()) {
                        builder.token(properties.token());
                    }
                    current = new MilvusClientV2(builder.build());
                    client = current;
                }
            }
        }
        return current;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    @PreDestroy
    public void close() {
        MilvusClientV2 current = client;
        if (current != null) {
            current.close();
        }
    }
}
