package com.eulerity.hackathon.imagefinder.crawler;

import java.io.IOException;
import java.net.URI;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class JsoupPageFetcher implements PageFetcher {
    private volatile long lastFetchAt;

    @Override
    public synchronized Document fetch(URI uri, int timeoutMillis, String userAgent) throws IOException {
        long now = System.currentTimeMillis();
        long elapsed = now - lastFetchAt;
        long minDelayMillis = 100L;
        if (elapsed < minDelayMillis) {
            try {
                Thread.sleep(minDelayMillis - elapsed);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        lastFetchAt = System.currentTimeMillis();
        return Jsoup.connect(uri.toString())
            .timeout(timeoutMillis)
            .userAgent(userAgent)
            .ignoreHttpErrors(true)
            .followRedirects(true)
            .get();
    }
}
