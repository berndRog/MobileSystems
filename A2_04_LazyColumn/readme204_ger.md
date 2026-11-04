# A2_04_LazyColumn

[English](readme204.md) | Deutsch

## Zielsetzung

Dieses Projekt stellt Personen in einer Compose-`LazyColumn` dar und verbindet Listenereignisse mit einem ViewModel im MVI-Stil und einem Room-Repository. Die früheren UI-Beispiele erhalten damit eine beobachtbare Datenquelle und Dependency Injection.

## Daten- und UI-Fluss

`MainApplikation` startet Koin und die Einrichtung der Beispieldaten. `SeedDatabase` fügt Beispielpersonen ein, wenn die Datenbank leer ist. `PersonRepository` bildet zwischen der Domain-Entity `Person` und dem DAO-DTO ab; `observeAll()` liefert Room-Änderungen als `Flow<Result<List<Person>>>`.

`PeopleViewModel` sammelt diesen Flow in `PeopleUiState`. `PeopleAdapter` beobachtet den Zustand mit `collectAsStateWithLifecycle()` und übergibt Liste und Callbacks an `PeopleScreen`. Der Screen stellt die Einträge in einer `LazyColumn` mit der `id` jeder Person als stabilem Schlüssel dar. Ein Löschereignis gelangt über `PeopleIntent.Remove` zum Repository; Room liefert anschließend die aktualisierte Liste. Der Detail-Intent wird derzeit nur geloggt; Navigation ist dafür noch nicht angeschlossen.

`PersonViewModel` und `PersonAdapter` bilden ein separates Beispiel für ein Erfassungs- und Detailformular. `MainActivity` zeigt derzeit `PeopleAdapter`; der Formularaufruf ist auskommentiert. `TestLazyColumn` in der Activity demonstriert `item`, indizierte `items(count = ...)` und `items(items = ..., key = ...)` mit einer Liste im Arbeitsspeicher.

## Ausprobieren

Öffne den Ordner als Android-Studio-Projekt oder führe aus:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Starte die App und entferne eine Beispielperson. Verfolge das Ereignis von der Zeile über ViewModel und DAO zurück zur beobachteten Liste. Vergleiche die `items`-Varianten von `TestLazyColumn` und erkläre die Bedeutung stabiler Schlüssel bei Änderungen der Liste. Die enthaltenen Tests sind Startbeispiele, keine gezielten Listen- oder Repository-Tests.
