package com.eulerity.hackathon.imagefinder;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.junit.Before;
import org.mockito.Mockito;

import com.eulerity.hackathon.imagefinder.crawler.ImageCrawlerService;
import com.google.gson.Gson;

public class ImageFinderTest {

	public HttpServletRequest request;
	public HttpServletResponse response;
	public StringWriter sw;
	public HttpSession session;

	@Before
	public void setUp() throws Exception {
		request = Mockito.mock(HttpServletRequest.class);
		response = Mockito.mock(HttpServletResponse.class);
    sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
		Mockito.when(response.getWriter()).thenReturn(pw);
		Mockito.when(request.getRequestURI()).thenReturn("/foo/foo/foo");
		Mockito.when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/foo/foo/foo"));
		session = Mockito.mock(HttpSession.class);
		Mockito.when(request.getSession()).thenReturn(session);
	}
	
  @Test
  public void testSuccessResponse() throws IOException, ServletException {
		ImageCrawlerService crawlerService = Mockito.mock(ImageCrawlerService.class);
		List<String> images = Arrays.asList("https://example.com/a.jpg", "https://example.com/b.png");
		Mockito.when(crawlerService.crawl("https://example.com")).thenReturn(images);

		Mockito.when(request.getServletPath()).thenReturn("/main");
		Mockito.when(request.getParameter("url")).thenReturn("https://example.com");

		new ImageFinder(crawlerService).doPost(request, response);
		Assert.assertEquals(new Gson().toJson(images), sw.toString());
  }

	@Test
	public void testMissingUrl() throws IOException, ServletException {
		ImageCrawlerService crawlerService = Mockito.mock(ImageCrawlerService.class);

		Mockito.when(request.getServletPath()).thenReturn("/main");
		Mockito.when(request.getParameter("url")).thenReturn("  ");

		new ImageFinder(crawlerService).doPost(request, response);

		Mockito.verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
	}
}



