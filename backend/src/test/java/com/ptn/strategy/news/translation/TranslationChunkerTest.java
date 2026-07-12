package com.ptn.strategy.news.translation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class TranslationChunkerTest {

    private final TranslationChunker chunker = new TranslationChunker();

    @Test
    void preservesParagraphOrderWithinSizeLimit() {
        List<String> chunks = chunker.chunk("First paragraph.\n\nSecond paragraph.\n\nThird.", 35);

        assertThat(chunks).containsExactly("First paragraph.\n\nSecond paragraph.", "Third.");
        assertThat(chunks).allMatch(chunk -> chunk.length() <= 35);
    }

    @Test
    void splitsAnOversizedParagraphWithoutDroppingText() {
        List<String> chunks = chunker.chunk("abcdefghij", 4);

        assertThat(chunks).containsExactly("abcd", "efgh", "ij");
        assertThat(String.join("", chunks)).isEqualTo("abcdefghij");
    }
}
