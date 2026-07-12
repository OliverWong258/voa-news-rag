package com.ptn.strategy.news.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UrlCanonicalizerTest {

    private final UrlCanonicalizer canonicalizer = new UrlCanonicalizer();

    @Test
    void canonicalizesVoaArticleAndRemovesTrackingData() {
        String result = canonicalizer.canonicalize(
                        "https://www.voanews.com/usa",
                        "http://voanews.com/a/example-story/8011681.html?utm_source=test#share")
                .orElseThrow();

        assertThat(result).isEqualTo("https://www.voanews.com/a/example-story/8011681.html");
        assertThat(canonicalizer.isArticleUrl(result)).isTrue();
    }

    @Test
    void resolvesRelativeArticleUrls() {
        assertThat(canonicalizer.canonicalize(
                        "https://www.voanews.com/usa",
                        "/a/example-story/8011681.html"))
                .contains("https://www.voanews.com/a/example-story/8011681.html");
    }

    @Test
    void rejectsExternalAndNonHttpUrls() {
        assertThat(canonicalizer.canonicalize(
                "https://www.voanews.com/", "https://example.com/a/story/1.html")).isEmpty();
        assertThat(canonicalizer.canonicalize(
                "https://www.voanews.com/", "javascript:alert(1)")).isEmpty();
    }

    @Test
    void distinguishesArticlesFromSectionAndVideoUrls() {
        assertThat(canonicalizer.isArticleUrl("https://www.voanews.com/usa")).isFalse();
        assertThat(canonicalizer.isArticleUrl("https://www.voanews.com/a/story/1234567.html")).isTrue();
        assertThat(canonicalizer.isArticleUrl("https://www.voanews.com/a/story/1234567.html/video")).isFalse();
    }
}
