package com.ptn.strategy.news.rag;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record RagQuestionRequest(
        @NotBlank String question,
        Integer topK,
        String category,
        LocalDate startDate,
        LocalDate endDate) {
}
