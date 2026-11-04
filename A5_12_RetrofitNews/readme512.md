# A5_12_RetrofitNews

English | [Deutsch](readme512_ger.md)

## Purpose

This project searches [News API](https://newsapi.org/docs/endpoints/everything) through Retrofit. **News API access is read-only:** the app sends a GET request for a search term and displays the returned articles. A user may save or remove selected articles locally in Room; those actions do not modify News API.

## Learning objectives

- Define a Retrofit GET endpoint with query parameters and add an API key through an OkHttp interceptor.
- Separate external response DTOs, the domain `Article`, and the locally persisted Room `ArticleDto`.
- Distinguish a one-time HTTP result from a Room `Flow` of saved articles.
- Handle loading, errors, and navigation in the ViewModel while keeping network and database code in repositories.

## Data flow

`NewsViewModel` validates the search text and calls `INewsRepository.search()`. `NewsRepository` trims the term, calls `INewsWebservice.getEverything()`, and maps the response to domain articles. `INewsWebservice` declares `GET v2/everything` with `q`, `page`, `pageSize`, and `sortBy=publishedAt`; `Globals.baseUrl` is `https://newsapi.org/`. The response DTOs tolerate missing fields. Mapping discards records without a usable URL and normalizes optional text.

Each search loads **one page**: the ViewModel uses the repository's default `page = 1`, and `Globals.pageSize` is 50. The request includes News API's `page` and `pageSize` parameters, but the app has no paging UI or load-more behavior. `NewsResponseDto.totalResults` is available in the response model but is not used to fetch further pages. [News API documents these parameters](https://newsapi.org/docs/endpoints/everything).

The UI shows search results from a single HTTP response. `NewsViewModel` keeps the search term, loading flag, and articles in `NewsUiState`; navigation and errors are one-time effects. The simple read operation goes straight through `INewsRepository`, without a separate use case.

## News API endpoints

News API offers two relevant read-only endpoints:

| Endpoint | Purpose | Used here? |
| --- | --- | --- |
| [`GET /v2/everything`](https://newsapi.org/docs/endpoints/everything) | Searches articles by `q`; results can be sorted with `sortBy`. | Yes. The app requests the first page, sorted by `publishedAt`. |
| [`GET /v2/top-headlines`](https://newsapi.org/docs/endpoints/top-headlines) | Returns current top headlines, filterable by country, category, or source, with an optional `q`. | No. This is an alternative endpoint for a different news view. |

Both endpoints return an article list, but the Retrofit interface in this project declares only `getEverything()`.

## Local saved articles

The article detail screen can save an article to Room. `ArticleRepository` uses `IArticleDao` for saving, removing, and observing the local collection. The article URL is the Room primary key, so saving the same URL does not create another row. The saved-articles screen observes Room changes through `Flow`; this is separate from the remote search response. `Article` is the common domain model, mapped explicitly from both the News API DTO and the Room DTO.

## API key and setup

Obtain a News API key and set `NEWS_API_KEY` in `A5_12_RetrofitNews/local.properties` or pass it as a Gradle property. The project also accepts the repository-root `local.properties` as a fallback. Do not commit the key. Gradle copies it into `BuildConfig.NEWS_API_KEY`, and `ApiKeyInterceptor` adds the `X-Api-Key` header to requests. Without a configured key, the interceptor sends the request without that header, and News API rejects unauthenticated access. See [News API authentication](https://newsapi.org/docs/authentication).

For example, add this line to the project-local file:

```properties
NEWS_API_KEY=your_key_here
```

Open this directory as an Android Studio project or run from this directory:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

The local unit tests use fake webservices and repositories to verify request mapping, saved-article behavior, and ViewModels without making live News API requests. Running the app's search requires internet access and a valid API key.

## Suggested learning path

1. Follow one search from `NewsIntent` through `NewsViewModel`, `INewsRepository`, `NewsRepository`, and `INewsWebservice`.
2. Compare a nullable `ArticleRemoteDto` with the validated domain `Article` and the Room `ArticleDto`.
3. Save a result, then compare the single remote search response with the locally observed saved-article list.
4. Identify where authentication, HTTP errors, and coroutine cancellation are handled.
