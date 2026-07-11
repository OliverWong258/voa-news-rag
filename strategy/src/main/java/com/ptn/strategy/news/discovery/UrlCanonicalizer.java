package com.ptn.strategy.news.discovery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UrlCanonicalizer {

    private static final Pattern VOA_ARTICLE_PATH = Pattern.compile("^/a/[^?#]+/\\d+\\.html$");

    public Optional<String> canonicalize(String baseUrl, String candidateUrl) {
        if (candidateUrl == null || candidateUrl.isBlank()) {
            return Optional.empty();
        }

        try {
            URI resolved = URI.create(baseUrl).resolve(candidateUrl.trim()).normalize();
            String host = resolved.getHost();
            if (host == null || !(host.equalsIgnoreCase("voanews.com")
                    || host.equalsIgnoreCase("www.voanews.com"))) {
                return Optional.empty();
            }

            String scheme = resolved.getScheme() == null
                    ? "https"
                    : resolved.getScheme().toLowerCase(Locale.ROOT);
            if (!scheme.equals("http") && !scheme.equals("https")) {
                return Optional.empty();
            }

            String path = resolved.getPath();
            if (path == null || path.isBlank()) {
                path = "/";
            } else if (path.length() > 1 && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            URI canonical = new URI("https", null, "www.voanews.com", -1, path, null, null);
            return Optional.of(canonical.toASCIIString());
        } catch (IllegalArgumentException | URISyntaxException exception) {
            return Optional.empty();
        }
    }

    public boolean isArticleUrl(String canonicalUrl) {
        try {
            return VOA_ARTICLE_PATH.matcher(URI.create(canonicalUrl).getPath()).matches();
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
