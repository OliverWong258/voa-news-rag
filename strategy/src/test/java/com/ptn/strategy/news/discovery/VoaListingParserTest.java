package com.ptn.strategy.news.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class VoaListingParserTest {

    private final VoaListingParser parser = new VoaListingParser(new UrlCanonicalizer());

    @Test
    void extractsUniqueVoaArticleLinksOnly() {
        String html = """
                <html><body>
                  <a href="/a/first-story/8011681.html">First</a>
                  <a href="https://www.voanews.com/a/second-story/8011682.html?ref=home">Second</a>
                  <a href="/a/first-story/8011681.html#share">Duplicate</a>
                  <a href="/usa">Section</a>
                  <a href="https://example.com/a/external/8011683.html">External</a>
                  <a href="javascript:void(0)">Invalid</a>
                </body></html>
                """;

        List<String> urls = parser.extractArticleUrls("https://www.voanews.com/", html);

        assertThat(urls).containsExactly(
                "https://www.voanews.com/a/first-story/8011681.html",
                "https://www.voanews.com/a/second-story/8011682.html");
    }
}
