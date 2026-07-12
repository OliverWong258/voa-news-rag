package com.ptn.strategy.news.rag;

import java.util.List;

public record RagAnswerResponse(String answer, boolean grounded, List<RagSource> sources) {
}
