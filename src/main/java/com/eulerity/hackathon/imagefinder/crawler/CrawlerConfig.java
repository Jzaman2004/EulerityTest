package com.eulerity.hackathon.imagefinder.crawler;

public class CrawlerConfig {
    private final int threadCount;
    private final int maxPages;
    private final int maxDepth;
    private final int requestTimeoutMillis;
    private final int requestDelayMillis;

    public CrawlerConfig(
        int threadCount,
        int maxPages,
        int maxDepth,
        int requestTimeoutMillis,
        int requestDelayMillis
    ) {
        this.threadCount = threadCount;
        this.maxPages = maxPages;
        this.maxDepth = maxDepth;
        this.requestTimeoutMillis = requestTimeoutMillis;
        this.requestDelayMillis = requestDelayMillis;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getRequestTimeoutMillis() {
        return requestTimeoutMillis;
    }

    public int getRequestDelayMillis() {
        return requestDelayMillis;
    }
}
