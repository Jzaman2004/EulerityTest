package com.eulerity.hackathon.imagefinder.crawler;

import java.io.IOException;
import java.net.URI;

import org.jsoup.nodes.Document;

public interface PageFetcher {
    Document fetch(URI uri, int timeoutMillis, String userAgent) throws IOException;
}
