# A5_12_RetrofitNews

[English](readme512.md) | Deutsch

## Zielsetzung

Dieses Projekt sucht Nachrichten über Retrofit in der [News API](https://newsapi.org/docs/endpoints/everything). **Der Zugriff auf News API ist nur lesend:** Die App sendet für einen Suchbegriff eine GET-Anfrage und zeigt die gelieferten Artikel an. Ausgewählte Artikel können lokal in Room gespeichert oder daraus entfernt werden; diese Aktionen ändern nichts an News API.

## Lernziele

- Einen Retrofit-GET-Endpunkt mit Query-Parametern definieren und den API-Schlüssel über einen OkHttp-Interceptor ergänzen.
- Externe Antwort-DTOs, die Domain-Entity `Article` und das lokal gespeicherte Room-`ArticleDto` unterscheiden.
- Eine einmalige HTTP-Antwort von einem Room-`Flow` für gespeicherte Artikel abgrenzen.
- Ladezustand, Fehler und Navigation im ViewModel behandeln, während Netzwerk- und Datenbankzugriffe in Repositories liegen.

## Datenfluss

`NewsViewModel` prüft den Suchtext und ruft `INewsRepository.search()` auf. `NewsRepository` trimmt den Begriff, ruft `INewsWebservice.getEverything()` auf und bildet die Antwort auf Domain-Artikel ab. `INewsWebservice` definiert `GET v2/everything` mit `q`, `page`, `pageSize` und `sortBy=publishedAt`; `Globals.baseUrl` lautet `https://newsapi.org/`. Die Antwort-DTOs erlauben fehlende Felder. Das Mapping verwirft Datensätze ohne verwendbare URL und normalisiert optionale Texte.

Jede Suche lädt **eine Seite**: Das ViewModel nutzt den Repository-Standardwert `page = 1`; `Globals.pageSize` ist 50. Die Anfrage enthält zwar die News-API-Parameter `page` und `pageSize`, aber die App besitzt weder eine Paging-Oberfläche noch ein Nachladen weiterer Seiten. `NewsResponseDto.totalResults` ist im Antwortmodell vorhanden, wird aber nicht zum Laden weiterer Seiten genutzt. Die [News-API-Dokumentation beschreibt diese Parameter](https://newsapi.org/docs/endpoints/everything).

Die Oberfläche zeigt Suchergebnisse aus einer einzelnen HTTP-Antwort. `NewsViewModel` hält Suchbegriff, Ladezustand und Artikel in `NewsUiState`; Navigation und Fehler sind einmalige Effects. Der einfache Lesezugriff führt direkt über `INewsRepository`, ohne eigenen Use Case.

## News-API-Endpunkte

News API bietet zwei relevante Endpunkte für lesende Zugriffe:

| Endpunkt | Zweck | Hier genutzt? |
| --- | --- | --- |
| [`GET /v2/everything`](https://newsapi.org/docs/endpoints/everything) | Sucht Artikel anhand von `q`; mit `sortBy` lassen sich die Ergebnisse sortieren. | Ja. Die App lädt die erste Seite, sortiert nach `publishedAt`. |
| [`GET /v2/top-headlines`](https://newsapi.org/docs/endpoints/top-headlines) | Liefert aktuelle Top-Schlagzeilen, filterbar nach Land, Kategorie oder Quelle, optional mit `q`. | Nein. Dies ist ein alternativer Endpunkt für eine andere Nachrichtenansicht. |

Beide Endpunkte liefern eine Artikelliste; das Retrofit-Interface dieses Projekts definiert jedoch nur `getEverything()`.

## Lokal gespeicherte Artikel

In der Artikeldetailansicht lässt sich ein Artikel in Room speichern. `ArticleRepository` nutzt `IArticleDao` zum Speichern, Entfernen und Beobachten der lokalen Sammlung. Die Artikel-URL ist der Room-Primärschlüssel; dieselbe URL erzeugt daher keinen zweiten Datensatz. Die Ansicht gespeicherter Artikel beobachtet Room-Änderungen über `Flow`. Das ist unabhängig von der Antwort der entfernten Suche. `Article` ist das gemeinsame Domain-Modell und wird sowohl aus dem News-API-DTO als auch aus dem Room-DTO explizit abgebildet.

## API-Schlüssel und Einrichtung

Besorge einen News-API-Schlüssel und setze `NEWS_API_KEY` in `A5_12_RetrofitNews/local.properties` oder übergib ihn als Gradle-Property. Die `local.properties` im Repository-Root dient ebenfalls als Fallback. Den Schlüssel nicht committen. Gradle übernimmt ihn in `BuildConfig.NEWS_API_KEY`; `ApiKeyInterceptor` ergänzt den Header `X-Api-Key`. Ohne konfigurierten Schlüssel wird die Anfrage ohne diesen Header gesendet und News API verweigert den nicht authentifizierten Zugriff. Siehe [Authentifizierung der News API](https://newsapi.org/docs/authentication).

Beispielsweise diese Zeile in die projektlokale Datei eintragen:

```properties
NEWS_API_KEY=your_key_here
```

Öffne diesen Ordner als Android-Studio-Projekt oder führe hier aus:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Die lokalen Unit-Tests verwenden Fake-Webservices und -Repositories. Sie prüfen Request-Mapping, das Verhalten gespeicherter Artikel und die ViewModels ohne echte News-API-Anfragen. Für die Suche in der laufenden App sind Internetzugang und ein gültiger API-Schlüssel erforderlich.

## Vorgeschlagener Lernpfad

1. Eine Suche von `NewsIntent` über `NewsViewModel`, `INewsRepository` und `NewsRepository` bis `INewsWebservice` verfolgen.
2. Ein nullable `ArticleRemoteDto` mit der validierten Domain-Entity `Article` und dem Room-`ArticleDto` vergleichen.
3. Ein Suchergebnis speichern und die einmalige entfernte Antwort mit der lokal beobachteten Artikelliste vergleichen.
4. Die Stellen für Authentifizierung, HTTP-Fehler und Coroutine-Cancellation finden.
