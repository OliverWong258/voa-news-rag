package com.ptn.strategy.news.discovery;

import java.util.LinkedHashSet;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class VoaListingParser {

    private final UrlCanonicalizer urlCanonicalizer;

    public VoaListingParser(UrlCanonicalizer urlCanonicalizer) {
        this.urlCanonicalizer = urlCanonicalizer;
    }

    public List<String> extractArticleUrls(String pageUrl, String html) {
        Document document = Jsoup.parse(html, pageUrl);
        LinkedHashSet<String> urls = new LinkedHashSet<>();

        document.select("a[href]").stream()
                .map(element -> element.attr("abs:href"))
                .map(url -> urlCanonicalizer.canonicalize(pageUrl, url))
                .flatMap(java.util.Optional::stream)
                .filter(urlCanonicalizer::isArticleUrl)
                .forEach(urls::add);

        return List.copyOf(urls);
    }
}
