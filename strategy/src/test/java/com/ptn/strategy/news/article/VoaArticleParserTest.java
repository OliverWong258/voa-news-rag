package com.ptn.strategy.news.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class VoaArticleParserTest {

    private final VoaArticleParser parser = new VoaArticleParser(new ObjectMapper());

    @Test
    void parsesNewsArticleJsonLd() {
        String html = """
                <html><head>
                  <script type="application/ld+json">
                    {
                      "@context": "https://schema.org",
                      "@type": "NewsArticle",
                      "headline": "VOA test headline",
                      "articleSection": ["Politics", "United States"],
                      "datePublished": "2026-07-11T08:30:00-04:00",
                      "author": {"@type": "Person", "name": "VOA Reporter"},
                      "articleBody": "This is the first paragraph of a sufficiently long VOA article. It contains enough text for parsing. This is the second paragraph with additional reporting details."
                    }
                  </script>
                </head><body></body></html>
                """;

        ParsedArticle article = parser.parse("https://www.voanews.com/a/test/1234567.html", html);

        assertThat(article.title()).isEqualTo("VOA test headline");
        assertThat(article.author()).isEqualTo("VOA Reporter");
        assertThat(article.publishedAt()).isEqualTo(LocalDateTime.of(2026, 7, 11, 12, 30));
        assertThat(article.content()).startsWith("This is the first paragraph");
        assertThat(article.category()).isEqualTo("Politics");
    }

    @Test
    void fallsBackToHtmlMetadataAndParagraphs() {
        String html = """
                <html><head>
                  <meta property="og:title" content="Fallback headline">
                  <meta name="author" content="VOA News">
                  <meta property="article:published_time" content="2026-07-11T12:00:00Z">
                  <meta property="article:section" content="Africa">
                </head><body>
                  <div class="wsw">
                    <p>This fallback paragraph contains the opening details of the news report and is deliberately long enough.</p>
                    <p>The second paragraph provides more context so the extracted article passes minimum validation.</p>
                  </div>
                </body></html>
                """;

        ParsedArticle article = parser.parse("https://www.voanews.com/a/test/1234567.html", html);

        assertThat(article.title()).isEqualTo("Fallback headline");
        assertThat(article.content()).contains("opening details", "more context");
        assertThat(article.publishedAt()).isEqualTo(LocalDateTime.of(2026, 7, 11, 12, 0));
        assertThat(article.category()).isEqualTo("Africa");
    }

    @Test
    void rejectsPagesWithoutArticleContent() {
        assertThatThrownBy(() -> parser.parse(
                "https://www.voanews.com/a/test/1234567.html", "<html><h1>Title only</h1></html>"))
                .isInstanceOf(ArticleParsingException.class)
                .hasMessageContaining("body");
    }
}
