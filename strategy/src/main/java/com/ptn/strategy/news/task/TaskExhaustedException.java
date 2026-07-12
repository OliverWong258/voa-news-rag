package com.ptn.strategy.news.task;

public class TaskExhaustedException extends RuntimeException {
    public TaskExhaustedException(long taskId) {
        super("Crawl task " + taskId + " exhausted its retry budget and must be moved to the DLQ");
    }
}
