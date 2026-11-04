# A5_11_PeopleImagesRetrofit

[English](readme511.md) | Deutsch

## Zielsetzung und Voraussetzungen

**A5_11 führt [A5_10_PeopleRetrofit](../A5_10_PeopleRetrofit/readme510_ger.md) fort, das die Room-Datenquelle aus [A5_01_PeopleRoom3](../A5_01_PeopleRoom3/readme501_ger.md) durch eine Web-API ersetzt hat.** UI, Use Cases und die Schnittstelle `IPersonRepository` bleiben vertraut. Neu sind Zuständigkeit und Übertragung der Bilddateien: A5_10 sendet nur einen lokalen Android-Pfad als JSON-String, während A5_11 eigene Endpunkte der `PeopleImagesApi` für Bild-Upload und Bildlöschung nutzt. Der Server speichert das Bild und liefert seine URL zurück.

Vorausgesetzt werden Repository und Retrofit aus A5_10, Multipart-HTTP-Anfragen und die Bildbearbeitung aus A5_01.

## Lernziele

Nach der Bearbeitung können Studierende:

- Erklären, warum ein lokaler Gerätepfad keine dauerhaft nutzbare Bildressource für andere Clients ist.
- JSON-Anfragen für Personendaten von Multipart-Uploads und Bildlöschung trennen.
- Einen temporären lokalen Bildpfad von einer HTTP(S)-URL eines Serverbilds unterscheiden.
- Create-, Update- und Delete-Abläufe über Client-Dateien und Serverressourcen verfolgen.
- Rollback nach einer fehlgeschlagenen zweiten Anfrage erklären und lokale Dateien erst nach Erfolg aufräumen.
- Bildverhalten im Repository und in Use Cases ohne laufenden Server testen.

## Personendaten und beobachtbarer Zustand

Wie in A5_10 nutzt `IPersonWebservice` Retrofit für GET, POST, PUT und DELETE auf `peopleapi/v1/people`. `PersonDto` ist der JSON-Vertrag und wird auf die Domain-Entity `Person` abgebildet; die UI hängt weiterhin von `IPersonRepository` ab. Anders als Room in A5_01 liefert ein HTTP-GET nur einen Snapshot. `PersonRepository` aktualisiert einen `MutableStateFlow` im Arbeitsspeicher und passt ihn nach erfolgreichen Änderungen an, damit `observeAll()` weiter funktioniert. Der Flow ist kein Offline-Speicher.

Koin stellt die projektspezifische Schnittstelle `IPersonWebservice` und das Repository bereit. `Shared` liefert Retrofit, OkHttp, JSON-Konvertierung und `NetworkExceptionMapper`. Netzwerkfehler werden in Ergebnisse für die Domain übersetzt; Coroutine-Cancellation wird weitergegeben.

Das separate C#-Projekt `PeopleImages` stellt den OpenAPI-v1-Vertrag der `PeopleImagesApi` bereit:

| Methode und Pfad | Zweck | Erfolgsstatus |
| --- | --- | --- |
| `GET /peopleapi/v1/people` | Alle Personen liefern | `200` |
| `GET /peopleapi/v1/people/count` | Anzahl der Personen liefern | `200` |
| `GET /peopleapi/v1/people/{id}` | Eine Person anhand ihrer UUID liefern | `200` |
| `POST /peopleapi/v1/people` | Eine Person aus JSON anlegen | `201` |
| `PUT /peopleapi/v1/people/{id}` | Bearbeitbare Felder per JSON ändern | `200` |
| `DELETE /peopleapi/v1/people/{id}` | Eine Person löschen | `204` |
| `POST /peopleapi/v1/people/{id}/image` | Ein Bild als Multipart-Feld `file` hochladen oder ersetzen | `200` |
| `DELETE /peopleapi/v1/people/{id}/image` | Das Bild einer Person entfernen | `200` |
| `GET /peopleapi/v1/images/{fileName}` | Gespeicherte Bilddaten herunterladen | `200` |

Die ID im Pfad ist eine UUID. Das JSON-`PersonDto` verlangt nicht leere Werte für `firstName` und `lastName`; `email`, `phone` und `imageUrl` dürfen null sein. JSON-Create ignoriert ein mitgesendetes `imageUrl`; JSON-Update erhält die vorhandene Server-Bild-URL. Ein Upload kann bei leerer, zu großer oder nicht unterstützter Datei mit `400` scheitern; fehlende Personen oder Bilder führen zu `404`. Weitere ungültige Eingaben oder Konflikte beim Anlegen können `400` oder `409` mit `ProblemDetails` als Fehlerantwort ergeben.

## Bildlebenszyklus in A5_11

Person und Bild sind getrennte Serverressourcen. `IPersonWebservice` ergänzt `POST peopleapi/v1/people/{id}/image` mit einem Multipart-Part namens `file` und `DELETE peopleapi/v1/people/{id}/image`. Beide Bildoperationen liefern ein aktualisiertes `PersonDto`. Der Server verwaltet `imageUrl`; Create- und Update-JSON-Anfragen setzen diesen Wert nicht direkt. Beim Mapping landet die zurückgegebene URL in `Person.imagePath`. Dieselbe Domain-Eigenschaft kann daher vor dem Upload vorübergehend einen lokalen Pfad und danach eine Server-URL enthalten.

Der Bild-Download-Endpunkt steht nicht in `IPersonWebservice`: Die App zeigt die zurückgegebene URL über ihren Bildlader an. Diese URL muss vom Android-Gerät aus erreichbar sein, nicht nur vom Rechner mit der API.

| Operation | Ablauf |
| --- | --- |
| Anlegen mit Bild | Person per JSON anlegen, danach die ausgewählte lokale Datei hochladen. Bei fehlgeschlagenem Upload versucht das Repository, die neu angelegte Person zu löschen. Die lokale Datei bleibt für einen erneuten Versuch erhalten. |
| Ändern mit neuem lokalem Bild | Personendaten per JSON ändern, danach die Ersatzdatei hochladen. Die lokale temporäre Datei erst nach vollständigem Erfolg entfernen. |
| Ändern mit vorhandener Server-URL | Personendaten ändern; das Serverbild beibehalten, ohne die URL als Datei hochzuladen. |
| Ändern ohne Bild | Personendaten ändern, danach den Bild-Löschendpunkt aufrufen. |
| Person löschen | Die Person auf dem Server löschen; das gespeicherte Bild gehört dem Server. Nur eine ausnahmsweise noch lokale temporäre Datei wird auf Android aufgeräumt. |

`ImageRequestPart` prüft, ob die lokale Datei existiert, bestimmt den Medientyp und erzeugt den Multipart-Part. HTTP(S)-URLs werden abgewiesen. `PersonUcCreate` und `PersonUcUpdate` löschen eine temporäre Picker-Datei erst nach Erfolg; bei einem Fehler bleibt sie für einen erneuten Versuch bestehen. Die Schritte bestehen aus unabhängigen HTTP-Anfragen. Ein fehlgeschlagenes Update kann deshalb einen erneuten Versuch erfordern und ist keine atomare Transaktion.

`SeedApi` prüft zuerst `countAll()`. Bei leerer API legt es Beispielpersonen per JSON an und lädt ihre erzeugten Bilder getrennt hoch. Nach Erfolg aller Anfragen löscht es die lokalen Seed-Dateien. Bei einem Fehler versucht es, die in diesem Seed-Lauf angelegten Personen wieder zu entfernen.

## Ausführen und prüfen

Starte das separate C#-Projekt `PeopleImages` auf dem Rechner mit der Bind-Adresse **`0.0.0.0:5081`**. `0.0.0.0` beschreibt, worauf der Server lauscht; diese Adresse gehört nicht in den Android-Client. Prüfe die API am Rechner mit `curl http://127.0.0.1:5081/peopleapi/v1/people/count`.

### Android Virtual Device (AVD)

Leite den Port des Emulators mit ADB zum Rechner weiter:

```bash
adb reverse tcp:5081 tcp:5081
```

Belasse `Globals.baseUrl` bei `http://127.0.0.1:5081/`. Durch `adb reverse` erreicht diese Adresse die API auf dem Rechner. Nach einem Neustart oder erneuten Verbinden des Emulators muss die Weiterleitung gegebenenfalls erneut eingerichtet werden.

### Testhandy im selben WLAN

Ermittle die **LAN-IP-Adresse des Rechners** in diesem WLAN, zum Beispiel `192.168.1.25`. Ändere `Globals.baseUrl` zu `http://192.168.1.25:5081/` und baue und installiere die App erneut. `127.0.0.1` auf dem Handy bezeichnet das Handy selbst. Für diesen WLAN-Zugriff ist kein `adb reverse` nötig. Erlaube eingehende Verbindungen auf TCP-Port 5081 in der Rechner-Firewall und prüfe, ob das WLAN Geräte voneinander isoliert. Ersetze die Beispiel-IP durch die tatsächliche Adresse.

Prüfe auch die zurückgegebene `imageUrl`: Enthält sie einen vom Handy nicht erreichbaren Hostnamen oder eine solche Adresse, können die Personendaten geladen werden, während das Bild fehlt. Die Bild-URL muss auf die API unter einer vom Handy erreichbaren Adresse zeigen, zum Beispiel der Rechner-LAN-IP. Das Manifest erlaubt den für dieses Lehrbeispiel verwendeten HTTP-Verkehr. Öffne diesen Ordner als eigenständiges Android-Studio-Projekt oder führe hier den Wrapper aus:

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest # Gerät oder Emulator erforderlich
```

`PersonRepositoryImageTest` verwendet einen Fake-Webservice und prüft Create/Upload, Rollback, Wiederverwendung von Server-URLs, Ersatz-Upload und Bildlöschung. Weitere lokale Tests prüfen Mapping, Use Cases, ViewModel-Verhalten, Navigation sowie erfolgreiches und fehlgeschlagenes Löschen im Repository. Sie benötigen keine laufende API. Der instrumentierte Test prüft die Swipe-Oberfläche. Bei einem Netzwerkfehler prüfe, ob der Dienst läuft und ob die AVD-Weiterleitung oder die in der App eingetragene Rechner-LAN-IP zur gewählten Variante passt.

## Vorgeschlagener Lernpfad

1. Die beiden `IPersonWebservice`-Schnittstellen vergleichen. Welche Bildendpunkte kommen in A5_11 hinzu, welche JSON-Endpunkte bleiben aus A5_10 erhalten?
2. Eine ausgewählte lokale Datei über `ImageRequestPart`, Multipart-Upload, das zurückgegebene `PersonDto.imageUrl` und das UI-Bild verfolgen.
3. Create mit fehlgeschlagenem Upload verfolgen und Rollback sowie Erhalt der lokalen Datei erklären.
4. Update mit lokalem Pfad, Server-URL und ohne Bild vergleichen. Die HTTP-Anfragen für jeden Fall vorhersagen.
5. Erklären, wem nach erfolgreichem Upload welche Ressource gehört und warum eine Server-URL nicht als lokale Datei gelöscht werden darf.
