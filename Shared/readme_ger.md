# Shared

[English](readme.md) | Deutsch

## Zweck und Einbindung

`Shared` ist die wiederverwendbare Android-Library, die jedes eigenständige App-Projekt als `:Shared` einbindet. Hier liegen Infrastruktur, Mediendienste und wiederverwendbare Compose-Komponenten; Fachregeln, Repositories, Screens und Webservice-Schnittstellen bleiben in den Apps.

Zum Arbeiten mit der Library einen **App-Ordner** in Android Studio öffnen. Shared hat keinen Launcher und keine eigenständigen Gradle-Settings. Die Settings jeder App binden `../Shared` und den gemeinsamen `build-logic`-Build ein; der Repository-Stamm ist kein gemeinsamer Build aller Apps.

Apps deklarieren:

~~~kotlin
implementation(project(":Shared"))
~~~

Die Library verwendet das Convention-Plugin `mobile.android.library` aus [build-logic](../build-logic/), den gemeinsamen [Versionskatalog](../gradle/libs.versions.toml) und die Abhängigkeiten in [build.gradle.kts](build.gradle.kts). Dazu gehören Compose, Navigation 3, Room 3 mit gebündeltem SQLite, Koin, Coil, Media3, Retrofit/OkHttp und Kotlin Serialization.

## Wichtige Bausteine

Die folgenden Quellpfade beziehen sich auf `src/main/java/de/rogallab/mobile/shared/`.

| Bereich | Wiederverwendbarer Code und Aufgabe |
| --- | --- |
| UI-Effects | [ui/effects](src/main/java/de/rogallab/mobile/shared/ui/effects/): `EffectDelegate`, `IEffectSource`, `EffectHandler` und `SnackbarController` unterstützen einmalige Ereignisse und die Snackbar-Behandlung. |
| Compose-Komponenten | [ui/components](src/main/java/de/rogallab/mobile/shared/ui/components/): `SwipeCard` erkennt Gesten, während Aufrufer den Karteninhalt liefern; `InputValueString` stellt wiederverwendbare Texteingabe bereit. `ScrollToItemIfNotVisible` unter `ui/lists` hilft beim Anzeigen wiederhergestellter Listeneinträge. |
| Vorläufiges Entfernen | [ui/removal](src/main/java/de/rogallab/mobile/shared/ui/removal/): `IVisualRemoval` und `VisualRemovalDelegate` verwalten ausgeblendete und ausstehende Einträge für Undo ohne Repository-Zugriff. |
| Bilder und externe Kamera | [ui/images](src/main/java/de/rogallab/mobile/shared/ui/images/): einfache/mehrfache Galerieauswahl, `CameraPickerHandler`, `ImageSelection`, `ImageRenderer` und `IImageEdit`/`ImageEdit`. |
| Bildspeicherung | [data/local/io](src/main/java/de/rogallab/mobile/shared/data/local/io/): `ImageFileStorage` speichert app-interne Bilder; `ImageMediaStore` verwaltet Bilder im gemeinsamen Medienspeicher. Schnittstellen und IO-Typen liegen unter `domain/images`. |
| Video | `VideoPickerHandlerSingle` wählt eine Video-URI; `VideoPlayer` zeigt das Video mit Media3 an. `IVideoMediaStore`/`VideoMediaStore` verwalten vorläufige, veröffentlichte und zu löschende MediaStore-Videos. |
| Kamera-Berechtigungsoberfläche | [ui/permissions](src/main/java/de/rogallab/mobile/shared/ui/permissions/): `CameraPermissionHandler`, `ButtonStepPermission` und `PermissionReport` liefern wiederverwendbare Bausteine für Berechtigungsoberflächen. |
| Persistenz | [data/local](src/main/java/de/rogallab/mobile/shared/data/local/): `AppDatabasePerson`, `IPersonDao`, `PersonDto`, Converter und konfigurierbare Room-3-Datenbankmodule. |
| Netzwerk | [data/network](src/main/java/de/rogallab/mobile/shared/data/network/): Verbindungsprüfung, Interceptor, Exception-Typen und `NetworkExceptionMapper`; `networkModule` setzt Retrofit, OkHttp und JSON-Konvertierung zusammen. |
| Hilfsfunktionen und Lifecycle-Logging | [domain/utilities](src/main/java/de/rogallab/mobile/shared/domain/utilities/): String-Provider, Logging, Eingabebereinigung, UUID-Konvertierung sowie Result- und Bildmodell-Hilfen. `BaseActivity` unter `ui` protokolliert Activity-Lifecycle-Callbacks. |

## Benötigte Koin-Module auswählen

Die Module unter [di](src/main/java/de/rogallab/mobile/shared/di/) werden gezielt eingebunden. Koin mit dem Android-Kontext der App konfigurieren und nur die für deren Funktionen benötigten Module registrieren:

| Factory | Registrierte Dienste |
| --- | --- |
| `utilitiesModule()` | `IStringProvider` auf Basis der Android-Stringressourcen. |
| `databaseModulePerson(databaseName, ioDispatcher)` | Die gemeinsame `AppDatabasePerson` und deren `IPersonDao`. Standard-Dispatcher ist `Dispatchers.IO`. |
| `databaseModule<TDatabase>(databaseName, queryCoroutineContext)` | Eine von der App gewählte Room-Datenbank mit `BundledSQLiteDriver`. Standard-Abfragekontext ist `Dispatchers.IO`; app-spezifische DAO-Bindings bleiben in der App. |
| `imageStorageModule(directoryName, ioDispatcher)` | `IImageFileStorage`, `IImageMediaStore`, **`IVideoMediaStore`** und `IImageEdit`. Standard-Dispatcher für Bild-IO ist `Dispatchers.IO`. |
| `networkModule(baseUrl, isDebug)` | Verbindungsprüfung/-Interceptor, Exception-Mapper, Logging, OkHttp, `Json` und Retrofit. |

Das zum Datenmodell passende Datenbankmodul wählen, statt überlappende Datenbankdefinitionen zu registrieren. API-Schnittstellen und deren Retrofit-Service-Bindings gehören in die App.

Das Netzwerkmodul verwendet Kotlin Serialization, ignoriert unbekannte JSON-Felder und setzt Timeouts von 10 Sekunden für Verbindungsaufbau, jeweils 30 Sekunden für Lesen/Schreiben und 60 Sekunden für den gesamten Aufruf. HTTP-Logging ist im Debug-Modus BASIC und sonst NONE.

## Bilder, Kamera und Video

Galerie-Picker liefern ausgewählte Content-URIs. `CameraPickerHandler` startet dagegen eine **externe Kamera-App** über `ActivityResultContracts.TakePicture`. Er bereitet eine app-interne Zieldatei vor, übergibt der Kamera eine FileProvider-URI und liefert über `onPhotoStored` den bestätigten internen Dateipfad zurück. Bei Abbruch/Fehler wird die vorläufige Datei aufgeräumt; Fehler werden über `onError` weitergegeben. Die Oberfläche erhält `CameraPickerActions` mit `takePhoto` und `isBusy`.

Das Library-[Manifest](src/main/AndroidManifest.xml) deklariert einen nicht exportierten FileProvider mit der Authority `${applicationId}.fileprovider` und temporären URI-Freigaben. Seine [Pfadkonfiguration](src/main/res/xml/image_file_paths.xml) stellt `filesDir` bereit; `ImageFileStorage` erstellt und validiert Bilder innerhalb seines konfigurierten Unterverzeichnisses. URI und interner Dateipfad beziehen sich auf dasselbe Bild, erfüllen aber unterschiedliche Aufgaben.

`CameraPermissionHandler` stellt eine Aktion zur Berechtigungsanfrage bereit und ruft `onGranted` auf, sobald die CAMERA-Berechtigung vorliegt. Er bietet außerdem Begründungs-/Einstellungsdialoge. App-spezifische Aufnahmeabläufe, die Verknüpfung der Funktionen und benötigte Manifest-Berechtigungen bleiben Aufgabe der verwendenden App; der externe Kamera-Picker ist von einer app-internen CameraX-Implementierung getrennt.

`VideoPickerHandlerSingle` verwendet den System-Medienpicker mit Beschränkung auf Videos. `VideoPlayer(uri, onClose, onSelectOther)` erstellt je URI einen Player, bereitet die Wiedergabe vor und startet sie. Er bietet Play/Pause sowie Auswahl-/Schließen-Aktionen, pausiert bei `ON_STOP` und gibt den Player beim Verlassen der Composition frei. Die Wiedergabeposition wird bei Neuerstellung nicht gespeichert.

Für die Videospeicherung erstellt `createPendingVideo` einen unsichtbaren Eintrag in der Movies-Sammlung und liefert ein `PendingVideo` mit beschreibbarem Dateideskriptor. Den Deskriptor nach Abschluss des Schreibens schließen und das fertige Video mit `publishVideo` veröffentlichen; zum Aufräumen `deleteVideoFromMediaStore` verwenden. Die Aufnahme selbst steuert die App.

## Tests und Aufrufe

Lokale Tests unter [src/test/java](src/test/java/) prüfen Hilfsfunktionen, DTOs/Converter, Bildspeicherung/-bearbeitung, DI-Module, Lifecycle-Logging, Effects, Snackbars und visuelles Entfernen. Lokale Tests mit Android-Abhängigkeiten verwenden Robolectric. Instrumentierte Tests unter [src/androidTest/java](src/androidTest/java/) prüfen Speicherung, Room-/DAO-Integration, Picker-Handler/-Ergebnisse und Compose-Komponenten.

Shared-Tests über die Gradle-Settings einer App starten, zum Beispiel vom Repository-Stamm aus:

~~~bash
./gradlew -p A3_04_SwipeGestures :Shared:testDebugUnitTest
ANDROID_SERIAL=emulator-5554 ./gradlew -p A3_04_SwipeGestures :Shared:connectedDebugAndroidTest
~~~

Unter Windows mit PowerShell:

~~~powershell
.\gradlew.bat -p A3_04_SwipeGestures :Shared:testDebugUnitTest
$env:ANDROID_SERIAL = "emulator-5554"
.\gradlew.bat -p A3_04_SwipeGestures :Shared:connectedDebugAndroidTest
Remove-Item Env:ANDROID_SERIAL
~~~

Builds/Tests benötigen die JDK- und Android-SDK-Konfiguration des Projekts; Gerätetests zusätzlich einen gestarteten Emulator oder ein autorisiertes Gerät. `ANDROID_SERIAL` wählt bei mehreren verbundenen Geräten eines aus.

Die Repository-Testskripte **prüfen Shared bereits einmal über die erste App** und danach die Apps. Bei Verwendung dieser Skripte ist kein zusätzlicher ausdrücklicher Shared-Lauf nötig:

~~~bash
./scripts/testDebugUnitTest.sh
./scripts/connectedDebugAndroidTest.sh
# Build, lokale Tests und Gerätetests in dieser Reihenfolge mit Zeitprotokoll:
./scripts/runDebugChecks.sh
~~~

~~~powershell
pwsh -NoProfile -File .\scripts_powershell\testDebugUnitTest.ps1
pwsh -NoProfile -File .\scripts_powershell\connectedDebugAndroidTest.ps1
pwsh -NoProfile -File .\scripts_powershell\runDebugChecks.ps1
~~~

Die [Bash-Anleitung](../scripts/ReadMe_ger.md) und [PowerShell-Anleitung](../scripts_powershell/ReadMe_ger.md) erklären Voraussetzungen, Fehlerverhalten und Zeitprotokolle. Shared-Berichte liegen üblicherweise unter `Shared/build/reports/tests/testDebugUnitTest/` und `Shared/build/reports/androidTests/connected/debug/`.

Nach Änderungen an gemeinsamem Verhalten sowohl die Library-Tests als auch eine App prüfen, die die betroffene Funktion verwendet. App-Builds nacheinander ausführen: Shared und build-logic teilen ihre Ausgabeverzeichnisse. Die Testübersicht beschreibt vorhandene Abdeckung, garantiert aber keinen eigenen Test für jeden Medien- oder Berechtigungsablauf.
