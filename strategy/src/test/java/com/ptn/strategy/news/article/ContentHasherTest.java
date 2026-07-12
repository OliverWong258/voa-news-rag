package com.ptn.strategy.news.article;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContentHasherTest {

    private final ContentHasher hasher = new ContentHasher();

    @Test
    void createsStableLowercaseSha256Hash() {
        assertThat(hasher.sha256("VOA news"))
                .isEqualTo("55f141b3dc0c06f0e82e038c03b319c638a6ebd50c0ebc5adeaa14c4f1ba2c49");
    }
}
