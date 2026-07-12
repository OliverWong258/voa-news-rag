package com.ptn.strategy.news.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class SemanticChunkerTest {

    private final SemanticChunker chunker = new SemanticChunker();

    @Test
    void chunksParagraphsWithBoundedOverlap() {
        String text = "第一段新闻内容。\n\n第二段包含更多事实。\n\n第三段是结尾。";

        List<String> chunks = chunker.chunk(text, 22, 5);

        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allMatch(chunk -> chunk.length() <= 22);
        assertThat(String.join(" ", chunks)).contains("第一段", "第二段", "第三段");
    }

    @Test
    void rejectsOverlapEqualToChunkSize() {
        assertThatThrownBy(() -> chunker.chunk("content", 10, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
