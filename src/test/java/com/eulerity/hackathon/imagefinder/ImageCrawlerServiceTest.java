package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.eulerity.hackathon.imagefinder.crawler.CrawlerConfig;
import com.eulerity.hackathon.imagefinder.crawler.ImageCrawlerService;
import com.eulerity.hackathon.imagefinder.crawler.JsoupPageFetcher;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ImageCrawlerServiceTest {
    private HttpServer server;
    private String baseUrl;
    private final Map<String, AtomicInteger> hitCount = new ConcurrentHashMap<String, AtomicInteger>();

    @Before
    public void setup() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", new TestSiteHandler(hitCount));
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    public void crawlsSubPagesAndAvoidsRecrawl() {
        ImageCrawlerService service = new ImageCrawlerService(
            new CrawlerConfig(4, 20, 3, 3000, 0),
            new JsoupPageFetcher()
        );

        List<String> images = service.crawl(baseUrl + "/");

        Assert.assertTrue(images.contains(baseUrl + "/img/root.jpg"));
        Assert.assertTrue(images.contains(baseUrl + "/img/a.png"));
        Assert.assertTrue(images.contains(baseUrl + "/img/b.png"));
        Assert.assertTrue(images.contains(baseUrl + "/img/shared.webp"));

        AtomicInteger sharedHits = hitCount.get("/shared");
        Assert.assertNotNull(sharedHits);
        Assert.assertEquals(1, sharedHits.get());
    }

    private static class TestSiteHandler implements HttpHandler {
        private final Map<String, AtomicInteger> hits;

        TestSiteHandler(Map<String, AtomicInteger> hits) {
            this.hits = hits;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            hits.computeIfAbsent(path, key -> new AtomicInteger()).incrementAndGet();

            String body;
            if ("/".equals(path)) {
                body = "<html><body>"
                    + "<img src='/img/root.jpg'/>"
                    + "<a href='/page-a'>A</a>"
                    + "<a href='/page-b'>B</a>"
                    + "<a href='https://example.com/outside'>Outside</a>"
                    + "</body></html>";
            } else if ("/page-a".equals(path)) {
                body = "<html><body><img src='/img/a.png'/><a href='/shared'>Shared</a></body></html>";
            } else if ("/page-b".equals(path)) {
                body = "<html><body><img src='/img/b.png'/><a href='/shared'>Shared</a></body></html>";
            } else if ("/shared".equals(path)) {
                body = "<html><body><img src='/img/shared.webp'/></body></html>";
            } else if (path.startsWith("/img/")) {
                body = "image";
            } else {
                body = "<html><body>Not Found</body></html>";
            }

            byte[] bytes = body.getBytes("UTF-8");
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
