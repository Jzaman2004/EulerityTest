# Eulerity ImageFinder - Submission Ready

This repository now contains a complete implementation of the Eulerity take-home crawler challenge.

## Where The Original Prompt Is
- Original challenge README has been preserved at:
  - `docs/question/README.original.md`

## Solution Docs
- Detailed solution notes are here:
  - `docs/solution/README.solution.md`

## Implemented Features
- Crawls images from the input URL page.
- Crawls sub-pages in the same domain.
- Uses multi-threaded crawling.
- Avoids re-crawling already visited pages.
- Enforces host/domain restrictions.
- Includes basic crawler friendliness controls:
  - max pages
  - max depth
  - timeout
  - delay between requests
- Handles common image sources:
  - `img[src]`
  - `source[srcset]`
  - icon links
  - `og:image` and `twitter:image`

## Build And Run
Requirements:
- Java 8
- Maven 3.5+

Commands:
```bash
mvn clean test package
mvn jetty:run
```

Open:
- http://localhost:8080

## Tests
Run all tests:
```bash
mvn test
```

## Submission Steps
1. Verify `test-links.txt` contains tested URLs.
2. Run final cleanup:
   - `mvn clean`
3. Zip the project folder.
4. Send the zip to Eulerity.
