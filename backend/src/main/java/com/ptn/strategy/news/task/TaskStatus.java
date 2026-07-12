package com.ptn.strategy.news.task;

public enum TaskStatus {
    PENDING,
    DISPATCHED,
    CRAWLING,
    CRAWLED,
    PROCESSING,
    COMPLETED,
    FAILED,
    RETRYING,
    DEAD
}
