package com.ptn.strategy.news.translation;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TranslationChunker {

    public List<String> chunk(String text, int maxChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            String normalized = paragraph.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.length() > maxChars) {
                flush(current, chunks);
                splitOversized(normalized, maxChars, chunks);
            } else if (current.isEmpty()) {
                current.append(normalized);
            } else if (current.length() + 2 + normalized.length() <= maxChars) {
                current.append("\n\n").append(normalized);
            } else {
                flush(current, chunks);
                current.append(normalized);
            }
        }
        flush(current, chunks);
        return List.copyOf(chunks);
    }

    private void splitOversized(String text, int maxChars, List<String> chunks) {
        for (int start = 0; start < text.length(); start += maxChars) {
            chunks.add(text.substring(start, Math.min(start + maxChars, text.length())));
        }
    }

    private void flush(StringBuilder current, List<String> chunks) {
        if (!current.isEmpty()) {
            chunks.add(current.toString());
            current.setLength(0);
        }
    }
}
