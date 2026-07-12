package com.ptn.strategy.news.translation;

public interface LlmClient {
    String translateToChinese(String englishText);

    String summarizeInChinese(String chineseText);
}
