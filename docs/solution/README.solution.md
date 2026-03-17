# Eulerity ImageFinder Solution Notes

This document explains the implemented solution for the take-home challenge.

## What Was Implemented
- Replaced the placeholder image response with a real crawler service.
- Added multi-threaded crawling using a fixed thread pool.
- Added same-domain enforcement (host must match the input URL host).
- Added re-crawl prevention using a thread-safe visited-page set.
- Added crawl safety controls:
  - max pages
  - max depth
  - request timeout
  - request delay
  - custom crawler user-agent
- Added support for multiple image sources:
  - `img[src]`
  - `source[srcset]`
  - icon links (`link[rel~=icon]`)
  - social metadata images (`og:image`, `twitter:image`)

## Code Structure
- Servlet entrypoint:
  - `src/main/java/com/eulerity/hackathon/imagefinder/ImageFinder.java`
- Crawler package:
  - `src/main/java/com/eulerity/hackathon/imagefinder/crawler/CrawlerConfig.java`
  - `src/main/java/com/eulerity/hackathon/imagefinder/crawler/PageFetcher.java`
  - `src/main/java/com/eulerity/hackathon/imagefinder/crawler/JsoupPageFetcher.java`
  - `src/main/java/com/eulerity/hackathon/imagefinder/crawler/ImageCrawlerService.java`
- Tests:
  - `src/test/java/com/eulerity/hackathon/imagefinder/ImageFinderTest.java`
  - `src/test/java/com/eulerity/hackathon/imagefinder/ImageCrawlerServiceTest.java`

## How It Works
1. Frontend sends `POST /main?url=<user-input-url>`.
2. `ImageFinder` validates input and invokes `ImageCrawlerService`.
3. `ImageCrawlerService`:
   - normalizes the input URL
   - crawls pages concurrently within the same host
   - skips already visited pages
   - collects image URLs and returns deduplicated results
4. Servlet returns JSON array to the frontend.

## Validation
- Unit test for servlet success and bad input handling.
- Integration-style crawler test using an in-memory local HTTP server to verify:
  - sub-page crawling
  - deduped page visits
  - multi-page image discovery

## Submission Checklist
1. Run tests:
   - `mvn test`
2. Run app:
   - `mvn clean test package jetty:run`
3. Verify manually on `http://localhost:8080`.
4. Ensure `test-links.txt` includes tested URLs.
5. Run final cleanup:
   - `mvn clean`
6. Zip the project folder and submit.
