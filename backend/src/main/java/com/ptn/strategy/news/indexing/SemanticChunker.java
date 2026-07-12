package com.ptn.strategy.news.indexing;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SemanticChunker {

    public List<String> chunk(String text, int maxChars, int overlapChars) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        if (overlapChars >= maxChars) {
            throw new IllegalArgumentException("Chunk overlap must be smaller than chunk size");
        }

        List<String> units = splitSemanticUnits(text);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String unit : units) {
            if (current.isEmpty()) {
                current.append(unit);
            } else if (current.length() + 1 + unit.length() <= maxChars) {
                current.append('\n').append(unit);
            } else {
                addWithHardLimit(current.toString(), maxChars, overlapChars, chunks);
                String overlap = suffix(current.toString(), overlapChars);
                current.setLength(0);
                if (!overlap.isBlank()) {
                    current.append(overlap).append('\n');
                }
                current.append(unit);
            }
        }
        if (!current.isEmpty()) {
            addWithHardLimit(current.toString(), maxChars, overlapChars, chunks);
        }
        return List.copyOf(chunks);
    }

    private List<String> splitSemanticUnits(String text) {
        List<String> units = new ArrayList<>();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            String normalized = paragraph.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            if (normalized.length() <= 600) {
                units.add(normalized);
            } else {
                for (String sentence : normalized.split("(?<=[。！？.!?])\\s*")) {
                    if (!sentence.isBlank()) {
                        units.add(sentence.trim());
                    }
                }
            }
        }
        return units;
    }

    private void addWithHardLimit(
            String value, int maxChars, int overlapChars, List<String> chunks) {
        String normalized = value.trim();
        if (normalized.length() <= maxChars) {
            chunks.add(normalized);
            return;
        }
        int step = maxChars - overlapChars;
        for (int start = 0; start < normalized.length(); start += step) {
            chunks.add(normalized.substring(start, Math.min(start + maxChars, normalized.length())));
            if (start + maxChars >= normalized.length()) {
                break;
            }
        }
    }

    private String suffix(String value, int length) {
        if (length <= 0 || value.isBlank()) {
            return "";
        }
        return value.substring(Math.max(0, value.length() - length)).trim();
    }
}
