# A5_13_UsedCars

[English](readme513.md) | Deutsch

## Zielsetzung

**A5_13 greift [A5_02_UsedCarsRoom3](../A5_02_UsedCarsRoom3/readme502_ger.md) auf: Die UsedCars-App bezieht Personen, Autos und Probefahrten jetzt über Retrofit und eine `UsedCarsApi` statt aus Room.** An denselben Fachobjekten lässt sich eine lokale mit einer entfernten Datenquelle vergleichen. Für die App muss die API laufen; der Zustand im Repository ist keine Offline-Datenbank.

## Beziehungen und Datenquelle

| Konzept | A5_02 (Room) | A5_13 (Retrofit) |
| --- | --- | --- |
| Personen, Autos, Probefahrten | Drei Entities, DAOs und eine `AppDatabase` | Drei serialisierbare DTOs, Retrofit-Webservices und Repositories |
| Verkäufer → Autos (1:N) | Fremdschlüssel `CarDto.personId`; Room-Abfragen und JOINs | `CarDto.personId` im JSON; `GET usedcarsapi/v1/people/{id}/cars` liefert die Autos eines Verkäufers |
| Person ↔ Auto über Probefahrt (m:n) | Zuordnungstabelle `TDriveDto` und JOINs | `TDriveDto` überträgt `personId` und `carId` in API-Anfragen und -Antworten |
| Beobachtung | Room meldet Änderungen der Datenbank | Repositories laden über HTTP neu und veröffentlichen den letzten Stand per `MutableStateFlow` |

Der **Verkäufer** eines Autos und die **teilnehmende Person** einer Probefahrt haben unterschiedliche Rollen. Eine Person kann mehrere Autos verkaufen und zugleich Autos anderer Personen probefahren. `TDriveDto` enthält zusätzlich `id`, `start: Instant` und `isCompleted`; `start` wird als ISO-8601-String serialisiert. Anders als die nullable Verkäuferspalte in A5_02 ist `CarDto.personId` in A5_13 ein erforderlicher `String`. Rooms Fremdschlüssel, Unique-Indizes und JOINs werden nicht mehr im Android-Client ausgeführt; die Datenintegrität auf API-Seite hängt vom Server ab.

## Anfragen und Repositories

`IPersonWebservice`, `ICarWebservice` und `ITDriveWebservice` definieren GET-, POST-, PUT- und DELETE-Anfragen unter `usedcarsapi/v1/`. Retrofit bildet JSON auf `PersonDto`, `CarDto` und `TDriveDto` ab; Mapping-Funktionen erzeugen daraus die Domain-Entities. Koin stellt die drei Webservices und Repositories bereit. Das gemeinsame Modul liefert Retrofit, OkHttp, JSON-Serialisierung und die Abbildung von Netzwerkfehlern.

Die UI verwendet weiter `IPersonRepository`, `ICarRepository` und `ITDriveRepository`. Ein HTTP-GET liefert nur einen Snapshot. Deshalb implementiert jedes Repository `observeAll()` mit einem `MutableStateFlow` und aktualisiert ihn nach erfolgreichen Schreibvorgängen. Änderungen anderer Clients werden erst durch eine neue Serverabfrage sichtbar. `CarRepository.findByPersonId()` nutzt den API-Endpunkt für die Autos eines Verkäufers statt eines lokalen JOINs.

Die separate C#-`UsedCarsApi` stellt folgenden OpenAPI-v1-Vertrag bereit. Alle Pfade beginnen mit `/usedcarsapi/v1`:

| Ressourcenpfad | Methoden und Ergebnisse |
| --- | --- |
| `/people` | `GET` alle (`200`), `POST` JSON-Person (`201`) |
| `/people/count` | `GET` Anzahl (`200`) |
| `/people/{id}` | `GET` eine (`200`), `PUT` JSON-Änderung (`200`), `DELETE` (`204`) |
| `/people/{id}/cars` | `GET` Autos dieses Verkäufers (`200`) |
| `/people/{id}/test-drive-cars` | `GET` über Probefahrten zugeordnete Autos (`200`) |
| `/people/{id}/image` | `POST` Multipart-Feld `file` zum Hochladen oder Ersetzen (`200`), `DELETE` Bild (`200`) |
| `/cars` | `GET` alle (`200`), `POST` JSON-Auto (`201`) |
| `/cars/count` | `GET` Anzahl (`200`) |
| `/cars/{id}` | `GET` eines (`200`), `PUT` JSON-Änderung (`200`), `DELETE` (`204`) |
| `/cars/{id}/images` | `POST` ein Multipart-Feld `file` hochladen (`200`) |
| `/cars/{id}/images/{fileName}` | `DELETE` ein Autobild (`200`) |
| `/tdrives` | `GET` alle (`200`), `POST` JSON-Probefahrt (`201`) |
| `/tdrives/count` | `GET` Anzahl (`200`) |
| `/tdrives/{id}` | `GET` eine (`200`), `PUT` JSON-Änderung (`200`), `DELETE` (`204`) |
| `/images/{fileName}` | `GET` gespeicherte Bilddaten (`200`; `404` falls nicht vorhanden) |

Die IDs im Pfad sind UUIDs. Die API-DTOs enthalten `PersonDto` mit einem nullable `imageUrl`, `CarDto` mit einer nullable Liste `imageUrls` und der Verkäufer-ID `personId` sowie `TDriveDto` mit Teilnehmer-`personId`, `carId`, `start` als Datum/Uhrzeit-String und `isCompleted`. Das Android-`CarDto` verlangt `personId` und stellt fehlende Bildlisten als leer dar. Beim JSON-Create einer Person wird ein mitgesendetes `imageUrl` ignoriert; JSON-Update erhält die vorhandene URL. Bilder werden über getrennte Endpunkte geändert. Ungültige Personenbild-Uploads können `400`, fehlende Personen oder Bilder `404` und Konflikte beim Anlegen einer Person `409` liefern; wo im Vertrag vorgesehen, enthalten Fehlerantworten `ProblemDetails`.

Der Server bietet außerdem `/people/{id}/test-drive-cars` an. Die aktuellen Android-Retrofit-Schnittstellen deklarieren die Verkäufer-Auto-Route, aber nicht diese Probefahrt-Auto-Route. Der Bild-Download-Endpunkt wird ebenfalls über zurückgegebene Bild-URLs statt einer eigenen Retrofit-Methode genutzt.

## Bilder und Beispieldaten

Eine Person hat ein Serverbild (`PersonDto.imageUrl`), ein Auto kann mehrere haben (`CarDto.imageUrls`). `imagePath` beziehungsweise `imagePaths` in den Domain-Entities können vorübergehend lokale Picker- oder Seed-Pfade enthalten; nach dem Upload enthalten sie Server-URLs. JSON-Anfragen und Bildübertragung sind getrennte Schritte:

- `POST /people/{id}/image` lädt ein Personenbild hoch; `DELETE /people/{id}/image` entfernt es.
- `POST /cars/{id}/images` lädt ein Autobild hoch; `DELETE /cars/{id}/images/{fileName}` entfernt eines.
- `PersonRepository` und `CarRepository` laden lokale Bilder nach dem JSON-Create hoch. Schlägt ein Upload fehl, versuchen sie, die neu angelegte Ressource zu löschen. Beim Auto-Update bleiben übernommene Server-URLs erhalten, entfernte Bilder werden gelöscht und neue lokale Dateien hochgeladen.

Diese Abläufe aus mehreren HTTP-Anfragen sind keine atomaren Transaktionen. `SeedApi` startet nur, wenn alle drei API-Sammlungen leer sind. Es legt zuerst Personen, dann Autos und anschließend Probefahrten an, lädt Bilder separat hoch und entfernt lokale temporäre Dateien nach Erfolg. Bei einem Fehler versucht es, Probefahrten, Autos und Personen in umgekehrter Reihenfolge wieder zu löschen.

Die zurückgegebenen Bild-URLs von Personen und Autos müssen vom Android-Gerät aus erreichbar sein. Eine erreichbare JSON-API reicht nicht aus, wenn die Bild-URLs eine unzugängliche Host-Adresse enthalten.

## Ausführen und testen

Starte die separate C#-`UsedCarsApi` auf dem Rechner mit der Bind-Adresse **`0.0.0.0:5082`**. `0.0.0.0` beschreibt, worauf der Server lauscht; diese Adresse gehört nicht in den Android-Client. Prüfe die API am Rechner mit `curl http://127.0.0.1:5082/usedcarsapi/v1/people/count`.

### Android Virtual Device (AVD)

Leite den Port des Emulators mit ADB zum Rechner weiter:

```bash
adb reverse tcp:5082 tcp:5082
```

Belasse `Globals.baseUrl` bei `http://127.0.0.1:5082/`. Durch `adb reverse` erreicht diese Adresse die API auf dem Rechner. Nach einem Neustart oder erneuten Verbinden des Emulators muss die Weiterleitung gegebenenfalls erneut eingerichtet werden.

### Testhandy im selben WLAN

Ermittle die **LAN-IP-Adresse des Rechners** in diesem WLAN, zum Beispiel `192.168.1.25`. Ändere `Globals.baseUrl` zu `http://192.168.1.25:5082/` und baue und installiere die App erneut. `127.0.0.1` auf dem Handy bezeichnet das Handy selbst. Für diesen WLAN-Zugriff ist kein `adb reverse` nötig. Erlaube eingehende Verbindungen auf TCP-Port 5082 in der Rechner-Firewall und prüfe, ob das WLAN Geräte voneinander isoliert. Ersetze die Beispiel-IP durch die tatsächliche Adresse. Auch zurückgegebene `imageUrl` und `imageUrls` müssen eine vom Handy erreichbare Host-Adresse verwenden.

Das Manifest erlaubt HTTP für dieses Lehrbeispiel. Im Projektordner:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

Lokale Unit-Tests prüfen DTO-Mapping, `Instant`-Serialisierung, Bildverhalten und Löschzustand der Repositories, Laden und Sortieren der Testfahrten sowie ausgewählte ViewModels. Sie verwenden bei Bedarf Fake-Webservices und benötigen keine laufende API. Für die Nutzung der App muss die API laufen; AVD-Weiterleitung oder die in der App eingetragene Rechner-LAN-IP müssen zur gewählten Variante passen.

## Vorgeschlagener Lernpfad

1. Die drei Room-DAOs aus A5_02 mit den drei Retrofit-Schnittstellen aus A5_13 vergleichen.
2. Ein Auto über seine Verkäufer-ID bis zu `GET /people/{id}/cars` verfolgen; anschließend eine Probefahrt über `personId` und `carId` nachvollziehen.
3. Create und Update mit Bildern über JSON, Multipart-Upload, zurückgegebene URLs und Repository-Zustand verfolgen.
4. Rooms Live-Abfragen und Datenbank-Constraints mit HTTP-Snapshots und serverseitiger Datenintegrität vergleichen.
