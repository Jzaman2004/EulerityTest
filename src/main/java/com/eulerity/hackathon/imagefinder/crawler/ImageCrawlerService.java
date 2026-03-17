package com.eulerity.hackathon.imagefinder.crawler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ImageCrawlerService {
    private static final String USER_AGENT = "EulerityImageFinderBot/1.0 (+https://eulerity.com)";

    private final CrawlerConfig config;
    private final PageFetcher pageFetcher;

    public ImageCrawlerService(CrawlerConfig config, PageFetcher pageFetcher) {
        this.config = config;
        this.pageFetcher = pageFetcher;
    }

    public List<String> crawl(String url) {
        URI rootUri = parseAndNormalize(url);
        String rootHost = rootUri.getHost();
        if (rootHost == null || rootHost.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid URL host: " + url);
        }

        Set<String> visitedPages = ConcurrentHashMap.newKeySet();
        Set<String> imageUrls = ConcurrentHashMap.newKeySet();

        ExecutorService executor = Executors.newFixedThreadPool(config.getThreadCount());
        CompletionService<CrawlPageResult> completion = new ExecutorCompletionService<CrawlPageResult>(executor);

        int submitted = 0;
        int inFlight = 0;

        visitedPages.add(rootUri.toString());
        completion.submit(new CrawlTask(rootUri, 0, rootHost));
        submitted++;
        inFlight++;

        try {
            while (inFlight > 0) {
                Future<CrawlPageResult> done = completion.take();
                inFlight--;

                CrawlPageResult result;
                try {
                    result = done.get();
                } catch (Exception ex) {
                    continue;
                }

                imageUrls.addAll(result.getImages());

                if (result.getDepth() >= config.getMaxDepth()) {
                    continue;
                }

                for (URI link : result.getNextLinks()) {
                    if (submitted >= config.getMaxPages()) {
                        break;
                    }

                    if (visitedPages.add(link.toString())) {
                        completion.submit(new CrawlTask(link, result.getDepth() + 1, rootHost));
                        submitted++;
                        inFlight++;
                    }
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        return new ArrayList<String>(new LinkedHashSet<String>(imageUrls));
    }

    private URI parseAndNormalize(String rawUrl) {
        String trimmed = rawUrl == null ? "" : rawUrl.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("URL must not be blank.");
        }

        String withScheme = trimmed.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$") ? trimmed : "https://" + trimmed;
        try {
            URI parsed = new URI(withScheme).normalize();
            if (parsed.getHost() == null) {
                throw new IllegalArgumentException("Malformed URL: " + rawUrl);
            }
            return stripFragment(parsed);
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Malformed URL: " + rawUrl, ex);
        }
    }

    private URI stripFragment(URI uri) {
        try {
            return new URI(
                uri.getScheme(),
                uri.getUserInfo(),
                uri.getHost(),
                uri.getPort(),
                uri.getPath(),
                uri.getQuery(),
                null
            ).normalize();
        } catch (URISyntaxException ex) {
            return uri;
        }
    }

    private boolean isSameHost(URI candidate, String rootHost) {
        String candidateHost = candidate.getHost();
        return candidateHost != null && candidateHost.equalsIgnoreCase(rootHost);
    }

    private boolean isHttp(URI uri) {
        String scheme = uri.getScheme();
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    private class CrawlTask implements java.util.concurrent.Callable<CrawlPageResult> {
        private final URI pageUri;
        private final int depth;
        private final String rootHost;

        CrawlTask(URI pageUri, int depth, String rootHost) {
            this.pageUri = pageUri;
            this.depth = depth;
            this.rootHost = rootHost;
        }

        @Override
        public CrawlPageResult call() {
            Set<String> images = new LinkedHashSet<String>();
            Set<URI> nextLinks = new LinkedHashSet<URI>();

            try {
                Document doc = pageFetcher.fetch(pageUri, config.getRequestTimeoutMillis(), USER_AGENT);
                collectImages(doc, images);
                collectLinks(doc, nextLinks, rootHost);
                applyRequestDelay();
            } catch (IOException ex) {
                // Skip pages that cannot be fetched.
            }

            return new CrawlPageResult(depth, images, nextLinks);
        }

        private void applyRequestDelay() {
            int delayMillis = config.getRequestDelayMillis();
            if (delayMillis <= 0) {
                return;
            }
            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void collectImages(Document doc, Set<String> imageUrls) {
        Elements imageElements = doc.select("img[src]");
        for (Element element : imageElements) {
            String abs = element.absUrl("src");
            if (isSupportedImageUrl(abs)) {
                imageUrls.add(abs);
            }
        }

        Elements srcsetElements = doc.select("source[srcset]");
        for (Element element : srcsetElements) {
            String srcset = element.attr("srcset");
            for (String candidate : srcset.split(",")) {
                String[] parts = candidate.trim().split("\\s+");
                if (parts.length > 0) {
                    String absolute = parts[0];
                    try {
                        absolute = new URI(doc.location()).resolve(parts[0]).toString();
                    } catch (Exception ex) {
                        // Keep original candidate when resolution fails.
                    }
                    if (isSupportedImageUrl(absolute)) {
                        imageUrls.add(absolute);
                    }
                }
            }
        }

        Elements iconLinks = doc.select("link[rel~=icon][href]");
        for (Element element : iconLinks) {
            String abs = element.absUrl("href");
            if (isSupportedImageUrl(abs)) {
                imageUrls.add(abs);
            }
        }

        Elements metaImage = doc.select("meta[property=og:image], meta[name=twitter:image]");
        for (Element element : metaImage) {
            String content = element.attr("content");
            if (content == null || content.trim().isEmpty()) {
                continue;
            }
            String absolute = content;
            try {
                absolute = new URI(doc.location()).resolve(content).toString();
            } catch (Exception ex) {
                // Keep original content when resolution fails.
            }
            if (isSupportedImageUrl(absolute)) {
                imageUrls.add(absolute);
            }
        }
    }

    private void collectLinks(Document doc, Set<URI> links, String rootHost) {
        Elements anchors = doc.select("a[href]");
        for (Element anchor : anchors) {
            String href = anchor.absUrl("href");
            if (href == null || href.trim().isEmpty()) {
                continue;
            }

            try {
                URI target = stripFragment(new URI(href).normalize());
                if (!isHttp(target)) {
                    continue;
                }
                if (!isSameHost(target, rootHost)) {
                    continue;
                }
                links.add(target);
            } catch (URISyntaxException ex) {
                // Skip invalid links.
            }
        }
    }

    private boolean isSupportedImageUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        String lower = url.toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://") || lower.endsWith(".png") || lower.endsWith(".jpg")
            || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp") || lower.endsWith(".bmp")
            || lower.endsWith(".svg") || lower.endsWith(".ico") || lower.contains("format=") || lower.contains("image");
    }

    private static class CrawlPageResult {
        private final int depth;
        private final Set<String> images;
        private final Set<URI> nextLinks;

        CrawlPageResult(int depth, Set<String> images, Set<URI> nextLinks) {
            this.depth = depth;
            this.images = images;
            this.nextLinks = nextLinks;
        }

        int getDepth() {
            return depth;
        }

        Set<String> getImages() {
            return images;
        }

        Set<URI> getNextLinks() {
            return nextLinks;
        }
    }
}
