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

**Beobachten:** Room → `IPersonDao.observeAll()` → `Flow<List<PersonDto>>` → Repository-Mapping → `Flow<Result<List<Person>>>` → ViewModel-State → Adapter → Liste.

`observeAll()` liest alle Personen sortiert nach `lastName, firstName`. Schreibzugriffe auf die Tabelle führen zu neuen Abfrageergebnissen im bestehenden Beobachtungspfad; der Adapter beobachtet UI-State lifecycle-aware.

Das DAO bietet außerdem `findById()`, `count()`, Inserts für eine Person oder eine Liste sowie Update und Delete. Inserts verwenden `OnConflictStrategy.ABORT`. Das Repository übersetzt Datenbankfehler in `Result.failure`; Coroutine-Cancellation wird weitergegeben.

## Didaktischer Ansatz

Der Anwendungsablauf bleibt vertraut; die Persistenzgrenze wird sichtbar gemacht. Die gemeinsame DAO-/Datenbankinfrastruktur aus A4_01 wird mit den modulinternen Klassen aus A5_01 verglichen. Room-Annotationen gehören zu `PersonDto`; die Domain-Entity `Person` bleibt davon frei. Mapping-Funktionen verdeutlichen diese Grenze.

`AppDatabase` registriert ausschließlich `PersonDto` und stellt `personDao()` bereit. In `di/appModules.kt` erzeugt Koin die Datenbank mit `Room.databaseBuilder`, `BundledSQLiteDriver` und einem Query-Coroutine-Kontext, standardmäßig `Dispatchers.IO`. Konfiguriert ist `people51.db`, Version 1. Anschließend wird das DAO an `PersonRepository` übergeben.

Die in A4_01 eingeführten Domain-Use-Cases behalten ihre Aufgabe: Personenpersistenz und Bild-Session-Commit beziehungsweise Aufräumen koordinieren. Sie rufen `IPersonRepository` auf; der konkrete Room-Zugriff gehört zur Data-Schicht.

`SeedDatabase` prüft `count()` und fügt Beispielpersonen nur bei leerer Tabelle ein. Damit steht ein Ausgangsdatenbestand bereit, ohne eine vorhandene, nicht leere Tabelle zu überschreiben.

## DI-Container für den Datenbankzugriff

Koin setzt die Datenbankabhängigkeiten in `di/appModules.kt` zusammen. `MainApplication` startet den Container mit `startKoin`, stellt `androidContext` bereit und lädt das Modul über `modules(appModule())`. Die Datenbankkonfiguration liegt damit im Composition Root von A5_01.

| Koin-Registrierung | Erzeugung und Abhängigkeit |
| --- | --- |
| `single<AppDatabase>` | `Room.databaseBuilder` mit `androidContext()`, `Globals.databaseName`, `BundledSQLiteDriver` und Query-Coroutine-Kontext |
| `single<IPersonDao>` | `get<AppDatabase>().personDao()` bezieht das DAO aus dieser Datenbank |
| `single<IPersonRepository>` | `PersonRepository(_personDao = get<IPersonDao>())` verbindet die Domain-Schnittstelle mit der konkreten Implementierung |

`single` hält pro Registrierung eine Instanz im Container; `get<T>()` löst eine registrierte Abhängigkeit auf. So entsteht die Kette **AppDatabase → IPersonDao → PersonRepository**, die von den konsumierenden Objekten gemeinsam verwendet wird. Koin ruft die konfigurierten Erzeugungsfunktionen auf und stellt Abhängigkeiten bereit; Room generiert die konkreten Datenbank-/DAO-Implementierungen.

Die `viewModel`-Definitionen beziehen `IPersonRepository` und übergeben es über Konstruktoren an ViewModels und Personen-Use-Cases. Die Empfänger erhalten ihre Abhängigkeiten, statt selbst Datenbanken zu erzeugen. Auch `SeedDatabase` erhält dieselbe registrierte Datenbank und dasselbe DAO; `MainApplication` bezieht es vor dem Seeding mit `get()`. `Shared` stellt weiterhin allgemeine Dienste wie Bildspeicher bereit, während die Datenbankregistrierungen lokal zu A5_01 gehören.

## Vorgeschlagener Lernpfad

1. **Schema lesen:** Jede Eigenschaft von `PersonDto` einer Spalte zuordnen; Identität über Primary Key, Nullability und Bildpfadreferenz erklären.
2. **DAO lesen:** Annotationen und SQL-Abfragen den jeweiligen Benutzeroperationen zuordnen.
3. **Schreiben und Beobachten verfolgen:** Einen Namen ändern und Mapping, Update sowie das neu sortierte Listenergebnis nachvollziehen.
4. **Persistenz prüfen:** Eine geänderte Person speichern, die App neu starten und die geladenen Werte vergleichen. Die Seed-Bedingung lesen und erklären, warum eine vorhandene, nicht leere Tabelle erhalten bleibt.
5. **DI verfolgen:** `startKoin` → `appModule()` → Datenbank → DAO → Repository → ViewModel/Use Case nachvollziehen. Erklären, welche Objekte als `single` registriert sind und warum die Empfänger nicht selbst eine Datenbank erzeugen.
6. **Grenzen und Fehler untersuchen:** `PersonRepositoryTest` lesen. Ein Fake-DAO prüft dort Fehler-/Erfolgsergebnisse für Update und Delete. Diese Repository-Tests von Tests echter Room-Abfragen unterscheiden.

Als Lernnachweis eignet sich ein kommentierter Schreib-/Lesezyklus mit Begründung der getrennten Aufgaben von DTO, Domain-Entity, DAO, Repository und Use Case. Der Schwerpunkt liegt auf einer Tabelle; Beziehungen und Joins gehören nicht zu diesem Lernschritt.

Quellcodebasis: [MobileSystems, Revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A5_01_PeopleRoom3).
