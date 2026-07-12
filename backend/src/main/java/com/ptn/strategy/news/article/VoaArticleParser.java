package com.ptn.strategy.news.article;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;

@Component
public class VoaArticleParser {

    private static final String BODY_SELECTORS = String.join(", ",
            "[data-qa='article-body']", ".article-body", ".wsw", "article .wysiwyg");

    private final ObjectMapper objectMapper;

    public VoaArticleParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedArticle parse(String url, String html) {
        Document document = Jsoup.parse(html, url);
        JsonNode newsJson = findNewsArticleJson(document);

        String title = firstNonBlank(
                text(newsJson, "headline"),
                meta(document, "meta[property=og:title]"),
                elementText(document.selectFirst("h1")));
        String content = firstNonBlank(
                text(newsJson, "articleBody"),
                extractBody(document));
        String author = firstNonBlank(
                nestedText(newsJson, "author", "name"),
                meta(document, "meta[name=author]"),
                elementText(document.selectFirst("[rel=author], .author")));
        String published = firstNonBlank(
                text(newsJson, "datePublished"),
                meta(document, "meta[property=article:published_time]"),
                attribute(document.selectFirst("time[datetime]"), "datetime"));
        String category = firstNonBlank(
                category(newsJson.get("articleSection")),
                meta(document, "meta[property=article:section]"),
                meta(document, "meta[name=section]"),
                elementText(document.selectFirst(
                        "[data-qa=breadcrumb] a:last-child, .breadcrumbs a:last-child")));

        if (title == null) {
            throw new ArticleParsingException("VOA article title was not found: " + url);
        }
        if (content == null || content.length() < 100) {
            throw new ArticleParsingException("VOA article body was missing or too short: " + url);
        }

        return new ParsedArticle(
                title, normalizeContent(content), author, parseDate(published), normalizeCategory(category));
    }

    private JsonNode findNewsArticleJson(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                JsonNode root = objectMapper.readTree(script.data());
                JsonNode match = findNewsArticleNode(root);
                if (match != null) {
                    return match;
                }
            } catch (Exception ignored) {
                // Invalid third-party JSON-LD should not prevent HTML fallback parsing.
            }
        }
        return objectMapper.createObjectNode();
    }

    private JsonNode findNewsArticleNode(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                JsonNode match = findNewsArticleNode(child);
                if (match != null) {
                    return match;
                }
            }
        } else if (node.isObject()) {
            JsonNode type = node.get("@type");
            if (type != null && (containsType(type, "NewsArticle") || containsType(type, "Article"))) {
                return node;
            }
            JsonNode graphMatch = findNewsArticleNode(node.get("@graph"));
            if (graphMatch != null) {
                return graphMatch;
            }
        }
        return null;
    }

    private boolean containsType(JsonNode type, String expected) {
        if (type.isTextual()) {
            return expected.equals(type.asText());
        }
        if (type.isArray()) {
            for (JsonNode value : type) {
                if (expected.equals(value.asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extractBody(Document document) {
        Element body = document.selectFirst(BODY_SELECTORS);
        if (body == null) {
            return null;
        }
        String paragraphs = body.select("p").stream()
                .map(Element::text)
                .filter(text -> !text.isBlank())
                .collect(Collectors.joining("\n\n"));
        return paragraphs.isBlank() ? body.text() : paragraphs;
    }

    private LocalDateTime parseDate(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
            } catch (DateTimeParseException invalidDate) {
                return null;
            }
        }
    }

    private String normalizeContent(String value) {
        return value.replace('\u00a0', ' ')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\s*\\n\\s*", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String meta(Document document, String selector) {
        return attribute(document.selectFirst(selector), "content");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isValueNode() ? null : blankToNull(value.asText());
    }

    private String nestedText(JsonNode node, String parent, String field) {
        JsonNode value = node.get(parent);
        if (value == null) {
            return null;
        }
        if (value.isArray() && !value.isEmpty()) {
            value = value.get(0);
        }
        return value.isObject() ? text(value, field) : null;
    }

    private String category(JsonNode value) {
        if (value == null) {
            return null;
        }
        if (value.isTextual()) {
            return blankToNull(value.asText());
        }
        if (value.isArray()) {
            for (JsonNode item : value) {
                if (item.isTextual() && blankToNull(item.asText()) != null) {
                    return item.asText();
                }
            }
        }
        return null;
    }

    private String normalizeCategory(String value) {
        String category = blankToNull(value);
        if (category == null) {
            return null;
        }
        category = category.replaceAll("\\s+", " ");
        return category.length() <= 128 ? category : category.substring(0, 128);
    }

    private String elementText(Element element) {
        return element == null ? null : blankToNull(element.text());
    }

    private String attribute(Element element, String name) {
        return element == null ? null : blankToNull(element.attr(name));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = blankToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
