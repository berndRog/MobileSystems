# A5_01_PeopleRoom3

## Zielsetzung und Voraussetzungen

**A5_01 baut auf A4_01_ImagePicker auf und stellt Room mit einer Tabelle in den Mittelpunkt.** Die vollständige Persistenzschicht gehört jetzt zum Beispielmodul: Datenbank, DAO, Persistenzmodell, Mapping und Repository. Damit lässt sich die lokale Speicherung nachvollziehen, ohne auf ein in `Shared` verborgenes Datenbankmodul zurückzugreifen.

Liste (`PeopleAdapter`/`PeopleScreen`), Formular (`PersonAdapter`/`PersonScreen`), Galerie-/Kameraablauf, Domain-Use-Cases, Navigation und bestätigtes Swipe-to-Delete bilden den vertrauten Anwendungsrahmen. Vorausgesetzt werden Kotlin-Data-Classes, SQL-Grundlagen, Coroutines/`Flow` und die Architektur aus A4_01.

## Lernziele

Nach der Bearbeitung können Studierende:

- Eine Room-Tabelle mit `@Entity` und `@PrimaryKey` einschließlich nullable Spalten definieren.
- Eine DAO-Schnittstelle mit `@Query`, `@Insert`, `@Update` und `@Delete` beschreiben.
- Reaktive Beobachtung über `Flow` von einzelnen `suspend`-Operationen unterscheiden.
- Erklären, wie `AppDatabase` Entities registriert und das DAO bereitstellt und wie Room Implementierungen über KSP erzeugt.
- Zwischen Persistenzmodell `PersonDto` und Domain-Entity `Person` mappen.
- Datenbank, DAO und Repository über Koin verbinden und UI sowie Domain-Use-Cases von Room-spezifischen Typen unabhängig halten.
- `single`, `get<T>()`, Constructor Injection und Container-Start erklären; nachvollziehen, wie dieselbe registrierte Datenbank DAO, Repository und Seeding versorgt.

## Eine Tabelle: Person

`data/local/dtos/PersonDto.kt` definiert `@Entity(tableName = "Person")`:

| Spalte | Kotlin-Typ | Bedeutung |
| --- | --- | --- |
| `id` | `String` | Primary Key |
| `firstName`, `lastName` | `String` | Erforderliche Namenswerte |
| `email`, `phone` | `String?` | Optionale Kontaktwerte |
| `imagePath` | `String?` | Optionaler Pfad zum separat gespeicherten Bild |

Die Tabelle speichert eine Bildreferenz; die Bilddatei bleibt im App-Speicher. So bleibt die Bildverwaltung aus A4_01 relevant, während eine Datenbank mit einer Tabelle untersucht wird.

## Konkreter Room-Datenfluss

**Schreiben:** Formular → Intent → ViewModel → Create-/Update-Use-Case → `IPersonRepository` → `PersonRepository` → `Person.toPersonDto()` → DAO → Room.

**Beobachten:** Room → `IPersonDao.observeAll()` → `Flow<List<PersonDto>>` → Projektions-Mapping in `PersonReadModel` → `Flow<Result<List<PersonListItem>>>` → State von `PeopleViewModel` → Adapter → Liste.

Das Repository lädt einzelne Domain-Entities mit `findById()` und führt Schreiboperationen aus. Das ReadModel beobachtet Anzeigeprojektionen über `IPersonReadModel.observeListItems()`.

`observeAll()` liest alle Personen sortiert nach `lastName, firstName`. Schreibzugriffe auf die Tabelle führen zu neuen Abfrageergebnissen im bestehenden Beobachtungspfad; der Adapter beobachtet UI-State lifecycle-aware.

Das DAO bietet außerdem `findById()`, `count()`, Inserts für eine Person oder eine Liste sowie Update und Delete. Inserts verwenden `OnConflictStrategy.ABORT`. Das Repository übersetzt Datenbankfehler in `Result.failure`; Coroutine-Cancellation wird weitergegeben.

## Didaktischer Ansatz

Der Anwendungsablauf bleibt vertraut; die Persistenzgrenze wird sichtbar gemacht. Die gemeinsame DAO-/Datenbankinfrastruktur aus A4_01 wird mit den modulinternen Klassen aus A5_01 verglichen. Room-Annotationen gehören zu `PersonDto`; die Domain-Entity `Person` bleibt davon frei. Mapping-Funktionen verdeutlichen diese Grenze.

`AppDatabase` registriert ausschließlich `PersonDto` und stellt `personDao()` bereit. In `di/appModules.kt` erzeugt Koin die Datenbank mit `Room.databaseBuilder`, `BundledSQLiteDriver` und einem Query-Coroutine-Kontext, standardmäßig `Dispatchers.IO`. Konfiguriert ist `people51.db`, Version 1. Anschließend wird das DAO an `PersonRepository` und `PersonReadModel` übergeben; nur das ReadModel benötigt den Dispatcher für die Beobachtung.

Die in A4_01 eingeführten Domain-Use-Cases behalten ihre Aufgabe: Personenpersistenz und Bild-Session-Commit beziehungsweise Aufräumen koordinieren. Sie rufen `IPersonRepository` auf; der konkrete Room-Zugriff gehört zur Data-Schicht.

`SeedDatabase` ist ein suspendierender Room-3-Callback für `onCreate(connection)`. Er verwendet die bereitgestellte Verbindung, vorhandene Converter und gebundene SQL-Parameter innerhalb der Erzeugungstransaktion. Er läuft genau beim erstmaligen Erstellen der Datenbank. Eine vorhandene oder absichtlich geleerte Datenbank wird beim erneuten Öffnen nicht neu befüllt. `MainApplication` öffnet die Datenbank kontrolliert asynchron; `resultOf()` protokolliert Fehler. Die Ausgangsdaten entsprechen A4, ohne bestehende Datenbanken zu überschreiben.

## DI-Container für den Datenbankzugriff

Koin setzt die Datenbankabhängigkeiten in `di/appModules.kt` zusammen. `MainApplication` startet den Container mit `startKoin`, stellt `androidContext` bereit und lädt das Modul über `modules(appModule())`. Die Datenbankkonfiguration liegt damit im Composition Root von A5_01.

| Koin-Registrierung | Erzeugung und Abhängigkeit |
| --- | --- |
| `single<AppDatabase>` | `Room.databaseBuilder` mit `androidContext()`, `Globals.databaseName`, `BundledSQLiteDriver` und Query-Coroutine-Kontext |
| `single<IPersonDao>` | `get<AppDatabase>().personDao()` bezieht das DAO aus dieser Datenbank |
| `single<IPersonReadModel>` | `PersonReadModel` erhält DAO und qualifizierten IO-Dispatcher für die Beobachtung von Anzeigeprojektionen |
| `single<IPersonRepository>` | `PersonRepository(_personDao = get<IPersonDao>())` verbindet die Domain-Schnittstelle mit der konkreten Implementierung |

`single` hält pro Registrierung eine Instanz im Container; `get<T>()` löst eine registrierte Abhängigkeit auf. So entsteht die Kette **AppDatabase → IPersonDao → PersonRepository**, die von den konsumierenden Objekten gemeinsam verwendet wird. Koin ruft die konfigurierten Erzeugungsfunktionen auf und stellt Abhängigkeiten bereit; Room generiert die konkreten Datenbank-/DAO-Implementierungen.

Die `viewModel`-Definitionen übergeben `IPersonReadModel` zur Listenbeobachtung an `PeopleViewModel`. `PersonViewModel` und die Personen-Use-Cases erhalten `IPersonRepository` aus `domain.repositories`. `SeedDatabase` erhält den Seed-Datenlieferanten und wird am Room-Builder als Erstellungs-Callback registriert. `MainApplication` öffnet die Datenbank über eine Reader-Verbindung, um die Erstellung auszulösen. Shared stellt weiterhin Bildspeicher und ImageEdit bereit.

## Erweiterungen gegenüber A4_01

Vier Modelle haben getrennte Aufgaben: `PersonForm` hält unveränderte Benutzereingaben, `Person` die fachliche Entität, `PersonListItem` die Anzeigeprojektion und `PersonDto` die Persistenzdaten. `PersonInputMapping.kt` bildet zwischen Form und Domain ab; `data/mapping/mappings.kt` enthält die DTO-/Projektions-Mapper. Normalisierung und Validierung erfolgen vor dem Speichern eines unveränderlichen Snapshots. Das Repository lädt Domain-Entities und schreibt; ausschließlich das ReadModel beobachtet die Liste. Die Sortierung nach Nachname und Vorname bleibt eine bewusste A5-Eigenschaft.

Kapitel 5 führt `try/catch/finally`, `getOrThrow()` und `resultOf()` ein. Einmaliges Laden und Speichern verwenden `getOrThrow()`, wenn Result-Fehler und geworfene Exceptions denselben Fehlerpfad haben. Flows verwenden weiterhin `.onSuccess`/`.onFailure`. Cancellation wird weitergeworfen. `finally` ersetzt beim Save-/Image-State die A4-Completion-Handler; `ensureActive()`, `cancelAndJoin()` und `_isClosing` bleiben für Koordination und verspätete Ergebnisse erforderlich. Die A4-Grenze für unerwartete Exceptions wird damit gezielt erweitert.

Bildverarbeitung und UI bleiben vertraut: `ImageEditDelegate` mit `REPLACE` und maximal einem Bild, gemeinsame Session für Delegate und Create-/Update-Use-Case, Persistierung vor Commit und Cleanup erst nach erfolgreichem Delete. Create/Update sind parametrisierte Koin-Factories mit `parametersOf(imageEdit)`; Delete ist eine eigenständige Factory und lädt die aktuelle Person anhand der ID. Picker bleiben komponiert; Save-Overlay, Bildfortschritt, Back-Sperren und Snackbar-Abstände entsprechen A4.

## Tests

Gemeinsame Verhaltensprüfungen decken Cancellation, Retry, Save/Cancel, Discard, Bildaktionen und die gemeinsame Session ab. Navigations- und Galerie-Integrationstests führen die A4-Prüfungen mit Form-/ReadModel-Anpassungen fort. Die Löschbestätigung wird mit dem echten StringProvider in Deutsch und Englisch geprüft. Zusätzliche A5-Tests behandeln Form-Mapping, ReadModel, Repository, DAO-Rückgabewerte, unerwartete Exceptions sowie einmalige Datenbankerstellung, Wiederöffnen geleerter Datenbanken und Rollback. Technische Shared-Bildtests werden nicht dupliziert.

## Vorgeschlagener Lernpfad

1. **Schema lesen:** Jede Eigenschaft von `PersonDto` einer Spalte zuordnen; Identität über Primary Key, Nullability und Bildpfadreferenz erklären.
2. **DAO lesen:** Annotationen und SQL-Abfragen den jeweiligen Benutzeroperationen zuordnen.
3. **Schreiben und Beobachten verfolgen:** Einen Namen ändern und Mapping, Update sowie das neu sortierte Listenergebnis nachvollziehen.
4. **Persistenz prüfen:** Eine geänderte Person speichern, die App neu starten und die geladenen Werte vergleichen. Den Erstellungs-Callback lesen und erklären, warum auch eine absichtlich geleerte Datenbank leer bleibt.
5. **DI verfolgen:** `startKoin` → `appModule()` → Datenbank → DAO → Repository → ViewModel/Use Case nachvollziehen. Erklären, welche Objekte als `single` registriert sind und warum die Empfänger nicht selbst eine Datenbank erzeugen.
6. **Grenzen und Fehler untersuchen:** `PersonRepositoryTest` lesen. Ein Fake-DAO prüft dort Fehler-/Erfolgsergebnisse für Update und Delete. Diese Repository-Tests von Tests echter Room-Abfragen unterscheiden.

Als Lernnachweis eignet sich ein kommentierter Schreib-/Lesezyklus mit Begründung der getrennten Aufgaben von DTO, Domain-Entity, DAO, Repository und Use Case. Der Schwerpunkt liegt auf einer Tabelle; Beziehungen und Joins gehören nicht zu diesem Lernschritt.

Quellcodebasis: [MobileSystems, Revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A5_01_PeopleRoom3).
