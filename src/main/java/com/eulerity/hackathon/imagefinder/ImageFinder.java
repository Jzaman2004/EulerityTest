package com.eulerity.hackathon.imagefinder;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.eulerity.hackathon.imagefinder.crawler.CrawlerConfig;
import com.eulerity.hackathon.imagefinder.crawler.ImageCrawlerService;
import com.eulerity.hackathon.imagefinder.crawler.JsoupPageFetcher;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@WebServlet(
    name = "ImageFinder",
    urlPatterns = {"/main"}
)
public class ImageFinder extends HttpServlet{
	private static final long serialVersionUID = 1L;

	protected static final Gson GSON = new GsonBuilder().create();
	private final ImageCrawlerService crawlerService;

	public ImageFinder() {
		this(new ImageCrawlerService(
			new CrawlerConfig(4, 50, 3, 5000, 150),
			new JsoupPageFetcher()
		));
	}

	ImageFinder(ImageCrawlerService crawlerService) {
		this.crawlerService = crawlerService;
	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("application/json");
		String path = req.getServletPath();
		String url = req.getParameter("url");
		System.out.println("Got request of:" + path + " with query param:" + url);

		if (url == null || url.trim().isEmpty()) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.getWriter().print(GSON.toJson(Collections.singletonMap("error", "Missing 'url' query parameter.")));
			return;
		}

		try {
			List<String> imageUrls = crawlerService.crawl(url);
			resp.getWriter().print(GSON.toJson(imageUrls));
		} catch (IllegalArgumentException ex) {
			resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			resp.getWriter().print(GSON.toJson(Collections.singletonMap("error", ex.getMessage())));
		} catch (Exception ex) {
			resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			resp.getWriter().print(GSON.toJson(Collections.singletonMap("error", "Crawl failed.")));
		}
	}
}
