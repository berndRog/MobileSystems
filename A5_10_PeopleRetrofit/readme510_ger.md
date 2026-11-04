# A5_10_PeopleRetrofit

[English](readme510.md) | Deutsch

## Zielsetzung und Voraussetzungen

**A5_10 führt [A5_01_PeopleRoom3](../A5_01_PeopleRoom3/readme501_ger.md) mit einer anderen Datenquelle fort.** Personenliste, Formular, Navigation, Bildauswahl, Use Cases und die Schnittstelle `IPersonRepository` bleiben vertraut. A5_01 implementiert sie mit einer lokalen Room-Datenbank und einem DAO; A5_10 verwendet Retrofit-Aufrufe an eine entfernte `PeopleApi`. Im Mittelpunkt steht die Grenze zwischen Domain-Logik und HTTP, nicht eine zweite Room-Datenbank.

Vorausgesetzt werden Repository und Dependency Injection aus A5_01, Kotlin-Coroutines/`Flow` sowie Grundlagen von JSON und HTTP-Methoden.

## Lernziele

Nach der Bearbeitung können Studierende:

- GET, POST, PUT und DELETE mit `@Path` und `@Body` in einer Retrofit-Schnittstelle abbilden.
- Das JSON-Transportmodell `PersonDto` durch Mapping-Funktionen von der Domain-Entity `Person` trennen.
- Erklären, warum eine einzelne HTTP-Antwort den beobachtbaren Room-`Flow` nicht direkt ersetzt.
- Nachvollziehen, wie Koin die gemeinsame Retrofit-/OkHttp-Infrastruktur, `IPersonWebservice` und `PersonRepository` verbindet.
- Eine lokale Bilddatei von dem in der `PeopleApi` gespeicherten Pfad-String unterscheiden.
- Coroutine-Cancellation erhalten und Netzwerkfehler vor dem ViewModel übersetzen.

## Von Room zur Web-API

| A5_01 | A5_10 |
| --- | --- |
| `IPersonDao` fragt die lokale Room-Tabelle ab. | `IPersonWebservice` sendet Anfragen an `PeopleApi`. |
| Room liefert nach Tabellenänderungen erneut `Flow<List<PersonDto>>`. | GET liefert einen Server-Snapshot. `PersonRepository` hält die zuletzt bekannte Liste in einem `MutableStateFlow` im Arbeitsspeicher. |
| Room-Annotationen beschreiben das Persistenzmodell. | `@Serializable` beschreibt das JSON-Transportmodell; Retrofit-Annotationen den HTTP-Vertrag. |

UI und Use Cases rufen weiterhin `IPersonRepository.observeAll()`, `findById()`, `create()`, `update()` und `remove()` auf. Beim Beobachten lädt das Repository die Liste vom Server. Nach erfolgreichem Anlegen, Ändern oder Löschen passt es den Zustand im Arbeitsspeicher an, damit aktive UI-Collector die Änderung erhalten. Dieser Zustand ist weder eine Offline-Datenbank noch ein dauerhafter Cache. Technische Fehler werden über `NetworkExceptionMapper` übersetzt; `CancellationException` wird weitergegeben.

`IPersonWebservice` nutzt `peopleapi/v1/people` für Liste, Anzahl und Anlegen; `peopleapi/v1/people/{id}` adressiert eine Person. `PersonDto.imageUrl` wird zu und von `Person.imagePath` gemappt.

Das separate C#-Projekt `PeopleApi` stellt folgenden ausschließlich JSON-basierten OpenAPI-v1-Vertrag bereit:

| Methode und Pfad | Zweck | Erfolgsstatus |
| --- | --- | --- |
| `GET /peopleapi/v1/people` | Alle Personen liefern | `200` |
| `GET /peopleapi/v1/people/count` | Anzahl der Personen liefern | `200` |
| `GET /peopleapi/v1/people/{id}` | Eine Person anhand ihrer UUID liefern | `200` |
| `POST /peopleapi/v1/people` | Eine Person aus JSON anlegen | `201` |
| `PUT /peopleapi/v1/people/{id}` | Bearbeitbare Felder per JSON ersetzen | `200` |
| `DELETE /peopleapi/v1/people/{id}` | Eine Person löschen | `204` |

Die ID im Pfad ist eine UUID. Das JSON-`PersonDto` enthält `id`, `firstName`, `lastName`, `email`, `phone` und das nullable Feld `imageUrl`. Bei Fehlern kann die API `ProblemDetails` liefern, etwa bei ungültigen Eingaben (`400`), fehlenden Personen (`404`), Konflikten beim Anlegen (`409`) oder Serverfehlern (`500`). `imageUrl` wird nur als String gespeichert; die API bietet keinen Endpunkt für das Übertragen oder Ausliefern von Bilddateien.

## Bildgrenze in A5_10

Bilder aus Galerie oder Kamera und für Seed-Daten erzeugte Bilder bleiben Dateien im privaten Android-App-Speicher (`people510`). Die JSON-Anfrage enthält **nur ihren lokalen Pfad als nullable `imageUrl`-String**. Die `PeopleApi` speichert diesen String; sie erhält keine Bilddaten und bietet in diesem Modul keinen Bild-Upload- oder Bild-Löschendpunkt. Ein Pfad von einem Gerät ist daher keine auf andere Clients übertragbare Bild-URL.

Die Create-/Update-Use-Cases übernehmen die lokale Bildbearbeitung erst nach erfolgreicher Personenanfrage. Beim Löschen wird zuerst der Serverdatensatz entfernt; die lokale Datei wird danach bestmöglich aufgeräumt. `SeedApi` fragt `countAll()` ab und legt Beispieldaten nur bei leerer API an. Die lokal erzeugten Bildpfade werden über denselben JSON-Personenendpunkt gesendet.

## Ausführen und prüfen

Starte das separate C#-Projekt `PeopleApi` auf dem Rechner mit der Bind-Adresse **`0.0.0.0:5080`**. `0.0.0.0` bezeichnet, worauf der Server lauscht; diese Adresse gehört nicht in den Android-Client. Prüfe die API am Rechner mit `curl http://127.0.0.1:5080/peopleapi/v1/people/count`.

### Android Virtual Device (AVD)

Leite den Port des Emulators mit ADB zum Rechner weiter:

```bash
adb reverse tcp:5080 tcp:5080
```

Belasse `Globals.baseUrl` bei `http://127.0.0.1:5080/`. Durch `adb reverse` erreicht diese Adresse die API auf dem Rechner. Nach einem Neustart oder erneuten Verbinden des Emulators muss die Weiterleitung gegebenenfalls erneut eingerichtet werden.

### Testhandy im selben WLAN

Ermittle die **LAN-IP-Adresse des Rechners** in diesem WLAN, zum Beispiel `192.168.1.25`. Ändere `Globals.baseUrl` zu `http://192.168.1.25:5080/` und baue und installiere die App erneut. `127.0.0.1` auf dem Handy bezeichnet das Handy selbst, nicht den Rechner. Für diesen WLAN-Zugriff ist kein `adb reverse` nötig. Erlaube eingehende Verbindungen auf TCP-Port 5080 in der Rechner-Firewall und prüfe, ob das WLAN Geräte voneinander isoliert; bei Verbindungsproblemen kann die URL im Handy-Browser getestet werden. Ersetze die Beispiel-IP durch die tatsächliche Adresse.

Das Manifest erlaubt den für dieses Lehrbeispiel verwendeten HTTP-Verkehr. Öffne diesen Ordner als eigenständiges Android-Studio-Projekt oder führe hier den Wrapper aus:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest # Gerät oder Emulator erforderlich
```

Die lokalen Tests prüfen DTO-Mapping, Use Cases, Navigation, ViewModel-Verhalten und Bildbearbeitung mit Fakes; sie benötigen keine laufende API. Der instrumentierte Test prüft die Swipe-Oberfläche. Bei einem Netzwerkfehler prüfe, ob der Dienst läuft und ob die AVD-Weiterleitung oder die in der App eingetragene Rechner-LAN-IP zur gewählten Variante passt.

## Vorgeschlagener Lernpfad

1. `IPersonDao` aus A5_01 mit `IPersonWebservice` vergleichen. Jede Repository-Operation einer HTTP-Methode und einem Endpunkt zuordnen.
2. Eine `Person` über `toPersonDto()`, die JSON-Anfrage, die Antwort und `toPerson()` verfolgen.
3. Einen Namen ändern und die Serverantwort bis zum `MutableStateFlow` des Repositorys und zur sichtbaren Liste verfolgen.
4. Ein Bild auswählen und bestimmen, wem die Datei gehört und was der Server tatsächlich speichert. Erklären, warum der Pfad nur im Speicherbereich dieses Clients nutzbar ist.
5. Die API trennen und untersuchen, wie `NetworkExceptionMapper` und die UI den Fehler behandeln, ohne Coroutine-Cancellation zu verändern.
