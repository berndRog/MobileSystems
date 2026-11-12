# MobileSystems

[English](readme.md) | Deutsch

Dieses Repository enthält eigenständige Android-App-Projekte (`A2_*`, `A3_*`, `A4_*`, `A5_*` und weitere Abschnitte) sowie die einmal vorhandene Android-Library `Shared`. Öffne in Android Studio **einen App-Ordner direkt**, zum Beispiel `A5_10_PeopleRetrofit`. Sein Gradle-Build bindet nur diese App und `Shared` ein; der Repository-Root ist kein Sammelprojekt.

## Frischen Clone einrichten

Installiere das Android-SDK und setze `ANDROID_HOME` auf seinen Pfad. Auf einem Mac mit dem üblichen SDK-Pfad von Android Studio:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
```

Verwende auf einem anderen Rechner dessen tatsächlichen SDK-Pfad. Die Builds benötigen die im Projekt konfigurierten SDK-Plattformen und Build-Tools. Gradle-Wrapper und Java-21-Toolchain sind im Repository konfiguriert; beim ersten Build können fehlende Komponenten heruntergeladen werden.

`local.properties` wird von Git ignoriert, auch symbolische Links mit diesem Namen. Nach einem frischen Clone fehlen deshalb sowohl die Datei im Repository-Root als auch die Dateien in den App-Ordnern. Mit einem gültigen `ANDROID_HOME` funktionieren die Build-Skripte trotzdem. Für Android Studio kannst du die IDE in jedem geöffneten App-Projekt eine `local.properties` erzeugen lassen oder dort eine Datei mit `sdk.dir=/absoluter/Pfad/zum/Android/sdk` anlegen. Ältere Apps übernehmen eine `local.properties` im Repository-Root nicht automatisch. Trage `sdk.dir` nicht in `gradle.properties` ein und committe keine rechnerabhängigen SDK-Pfade.

## Lernprogression

Die Projekte bauen aufeinander auf. Jeder Schritt ergänzt einen fachlichen Schwerpunkt:

- `A2_01_Count`: beobachtbaren Compose-Zustand, State Hoisting und Rekomposition kennenlernen.
- `A2_02_Layout`: Inhalte mit `Column`, `Row`, `Box` und `FlowRow` anordnen und Bildskalierung vergleichen.
- `A2_03_TextField`: Texteingaben an Stateholder, MVVM und MVI anbinden.
- `A2_04_LazyColumn`: Listen mit stabilen Schlüsseln darstellen und Listenereignisse mit beobachteten Room-Daten verbinden.
- `A3_01_Material`: ein Material-Formular, Mapping und Validierung ergänzen.
- `A3_02_EffectHandling`: dauerhaften UI-Zustand von einmaligen Meldungen und Fehlern trennen.
- `A3_03_Navigation`: Navigation-Effects über Navigation 3 ausführen.
- `A3_04_SwipeGestures`: Wischen zum Öffnen von Wischen mit Löschbestätigung unterscheiden.
- `A3_05_SwipeDeleteUndo`: sofortiges visuelles Entfernen, verzögertes Löschen, Undo und Wiederherstellung ergänzen.
- `A4_01_ImagePicker`: Bilder auswählen und temporäre oder ersetzte Dateien je nach Speicherergebnis verwalten.
- `A5_01_PeopleRoom3`: Personen mit Room 3 hinter einem Repository speichern.
- `A5_02_UsedCarsRoom3`: Verkäufer, Autos und Probefahrten mit Room-Beziehungen modellieren.
- `A5_10_PeopleRetrofit`: Room durch eine Personen-Web-API ersetzen, die nur Bildpfade überträgt.
- `A5_11_PeopleImagesRetrofit`: Bild-Upload und -Löschung über eigene API-Endpunkte ergänzen.
- `A5_12_RetrofitNews`: lesende News-API-Suche mit lokal gespeicherten Room-Artikeln verbinden.
- `A5_13_UsedCars`: das Personen-, Auto- und Probefahrtmodell von Room auf Retrofit umstellen.

## Projektanleitungen

Zu jedem App-Projekt und zur Library `Shared` gibt es eine englische und eine deutsche Anleitung. Die Reihenfolge der Apps führt von Compose-Grundlagen über Effects, Navigation und Bilder zu Room und Web-APIs:

| Projekt | Schwerpunkt | Anleitungen |
| --- | --- | --- |
| `A2_01_Count` | Zählerzustand, State Hoisting und Rekomposition | [English](A2_01_Count/readme201.md) · [Deutsch](A2_01_Count/readme201_ger.md) |
| `A2_02_Layout` | `Column`, `Row`, `Box`, `FlowRow` und Bilder | [English](A2_02_Layout/readme202.md) · [Deutsch](A2_02_Layout/readme202_ger.md) |
| `A2_03_TextField` | Texteingaben mit Stateholder-, MVVM- und MVI-Variante | [English](A2_03_TextField/readme203.md) · [Deutsch](A2_03_TextField/readme203_ger.md) |
| `A2_04_LazyColumn` | Personenliste mit stabilen Schlüsseln, Room-Beobachtung und Listenereignisse | [English](A2_04_LazyColumn/readme204.md) · [Deutsch](A2_04_LazyColumn/readme204_ger.md) |
| `A3_01_Material` | Material-Formular, Mapping und Validierung | [English](A3_01_Material/readme301.md) · [Deutsch](A3_01_Material/readme301_ger.md) |
| `A3_02_EffectHandling` | Einmalige UI-Effects | [English](A3_02_EffectHandling/readme302.md) · [Deutsch](A3_02_EffectHandling/readme302_ger.md) |
| `A3_03_Navigation` | Navigation 3 und Navigation-Effects | [English](A3_03_Navigation/readme303.md) · [Deutsch](A3_03_Navigation/readme303_ger.md) |
| `A3_04_SwipeGestures` | Swipe-Aktionen und Löschbestätigung | [English](A3_04_SwipeGestures/readme304.md) · [Deutsch](A3_04_SwipeGestures/readme304_ger.md) |
| `A3_05_SwipeDeleteUndo` | Visuelles Entfernen und Undo | [English](A3_05_SwipeDeleteUndo/readme305.md) · [Deutsch](A3_05_SwipeDeleteUndo/readme305_ger.md) |
| `A4_01_ImagePicker` | Bildauswahl und Dateilebenszyklus | [English](A4_01_ImagePicker/readme401.md) · [Deutsch](A4_01_ImagePicker/readme401_ger.md) |
| `A5_01_PeopleRoom3` | Personen in Room 3 speichern | [English](A5_01_PeopleRoom3/readme501.md) · [Deutsch](A5_01_PeopleRoom3/readme501_ger.md) |
| `A5_02_UsedCarsRoom3` | Verkäufer, Autos und Probefahrten mit Room-Beziehungen | [English](A5_02_UsedCarsRoom3/readme502.md) · [Deutsch](A5_02_UsedCarsRoom3/readme502_ger.md) |
| `A5_10_PeopleRetrofit` | Personen über eine Web-API; nur Bildpfade werden gespeichert | [English](A5_10_PeopleRetrofit/readme510.md) · [Deutsch](A5_10_PeopleRetrofit/readme510_ger.md) |
| `A5_11_PeopleImagesRetrofit` | Personen-API mit eigenen Endpunkten für Bild-Upload und -Löschung | [English](A5_11_PeopleImagesRetrofit/readme511.md) · [Deutsch](A5_11_PeopleImagesRetrofit/readme511_ger.md) |
| `A5_12_RetrofitNews` | Lesende News-API-Suche; ausgewählte Artikel lokal in Room gespeichert | [English](A5_12_RetrofitNews/readme512.md) · [Deutsch](A5_12_RetrofitNews/readme512_ger.md) |
| `A5_13_UsedCars` | Personen, Autos und Probefahrten aus A5_02 über Retrofit | [English](A5_13_UsedCars/readme513.md) · [Deutsch](A5_13_UsedCars/readme513_ger.md) |
| `Shared` (Library) | Wiederverwendbare UI-, Bild-, Room-, Netzwerk- und DI-Bausteine | [English](Shared/readme.md) · [Deutsch](Shared/readme_ger.md) |

## Teststruktur

Die Tests verwenden die üblichen Android-Source-Sets: `src/test/java` für lokale JVM-Tests und `src/androidTest/java` für Tests, die Android oder Compose auf einem Gerät benötigen. Die Test-Packages spiegeln die Production-Packages. Modullokale Hilfen liegen unter `de.rogallab.mobile.testing`.

- `A2_01_Count`: lokaler Test des ViewModel-Zustands und instrumentierter Test des Zähler-Buttons.
- `A2_02_Layout`: ein instrumentierter Test prüft Überschrift und vertikale Reihenfolge der drei Boxen; das Layout-Beispiel hat keinen eigenen lokalen JVM-Test.
- `A2_03_TextField`: lokale Tests der MVVM-/MVI-Zustände und ein instrumentierter Formular-Test für die Aktivierung des Speichern-Buttons.
- `A2_04_LazyColumn`: lokaler Test des Personen-Mappings und instrumentierter Test der Interaktionen mit einer Listenkarte.
- `A3_01_Material`: Mapping, Validierung und grundlegender ViewModel-Zustand.
- `A3_02_EffectHandling`: einmalige `ShowMessage`- und `ShowError`-Effects.
- `A3_03_Navigation`: Navigation- und Back-Effects sowie Ladefehler. Die App verwendet Material 3 `SnackbarHostState` direkt und hat keinen eigenen Snackbar-Coordinator-Test.
- `A3_04_SwipeGestures`: Bestätigungsverhalten im ViewModel und instrumentierte Swipe-Tests in beide Richtungen.
- `A3_05_SwipeDeleteUndo`: Undo und Wiederherstellung im ViewModel sowie instrumentierte Swipe- und Listen-Tests.
- `A4_01_ImagePicker`: Bild- und Use-Case-Verhalten bei erfolgreichen und fehlgeschlagenen Speichervorgängen sowie Navigations- und Swipe-Tests.
- `A5_01_PeopleRoom3`: Repository-, Use-Case-, Bild-, ViewModel- und Swipe-Verhalten.
- `A5_02_UsedCarsRoom3`: Room-DAO- und Beziehungstests, Converter, Repository-Verhalten und Löschabläufe.
- `A5_10_PeopleRetrofit`: Remote-Mapping, Bildpfade, Repository-Zustand beim Löschen, Use Cases, Navigation und Swipe-Verhalten.
- `A5_11_PeopleImagesRetrofit`: Bild-Upload und -Löschung, Repository-Zustand beim Löschen, Mapping, Use Cases und UI-Verhalten.
- `A5_12_RetrofitNews`: beide Repositories und drei Feature-ViewModels; der Android-Test ist ein Startbeispiel.
- `A5_13_UsedCars`: Remote-Mapping, `Instant`-Serialisierung, Bildverarbeitung, Repository-Zustand beim Löschen, Laden und Sortieren der Testfahrten sowie Listen-ViewModels; derzeit gibt es keine Tests unter `src/androidTest`.
- `Shared`: lokale Tests prüfen Hilfsfunktionen, Speicherung, Bildbearbeitung, `SnackbarController`, `EffectDelegate` und visuelles Entfernen. Instrumentierte Tests prüfen Android-Speicherung, Datenbankintegration, Bildauswahl und Compose-Komponenten.

Die Apps verwenden `targetSdk = 37`. Android-nahe lokale Tests mit Robolectric 4.16.1 wählen über `@Config(sdk = [35])` die unterstützte Test-API 35; das ändert die Produktionskonfiguration nicht. `MainDispatcherRule` schaltet `Alog` auf Nicht-Android-Ausgabe, damit reine JVM-Tests nicht `android.util.Log` aufrufen.

Tests müssen nicht direkt neben den Production-Dateien liegen, aber einem Gradle-Test-Source-Set zugeordnet sein. Größere Projekte können Gradle `testFixtures` oder ein separates Test-Support-Modul verwenden. In diesen Lehrprojekten bleiben die wenigen Hilfsklassen in den einzelnen Modulen.

### Alle Apps bauen und testen

Führe die drei Skripte im Repository-Root der Reihe nach aus. Jedes Skript lässt sich auch einzeln oder aus einem anderen Arbeitsverzeichnis starten.

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk" # an deinen Rechner anpassen
./scripts/assembleDebug.sh && \
./scripts/testDebugUnitTest.sh && \
./scripts/connectedDebugAndroidTest.sh
```

1. `assemble.sh` löscht den lokalen Gradle-Configuration-Cache jeder App, gibt ihren Namen aus und führt `assembleDebug` aus.
2. `test.sh` gibt den App-Namen aus und führt die lokalen JVM-Tests mit `testDebugUnitTest` aus.
3. `androidtest.sh` gibt den App-Namen aus und führt `connectedDebugAndroidTest` auf einem verbundenen Gerät oder Emulator aus. Apps ohne Android-Tests werden als übersprungen markiert. Fehlen bei einer App mit Android-Tests die Testergebnisse, meldet das Skript auch dann einen Fehler, wenn Gradle mit Exit-Code 0 endet.

Jedes Skript stoppt beim ersten Fehler. Durch `&&` beginnt die nächste Phase nur nach erfolgreichem Abschluss der vorherigen. Gelöscht wird nur der Configuration Cache der Apps; der globale Gradle-Cache mit heruntergeladenen Abhängigkeiten bleibt erhalten. Die Skripte verwenden `--quiet --warning-mode=none`: Übliche Gradle-Meldungen werden ausgeblendet, Fehler bleiben sichtbar. Zur Diagnose kannst du die Gradle-Aufgabe einer App ohne diese Optionen starten.

Die Skripte durchlaufen App-Ordner; die Tests der Library werden separat gestartet, wie in der [Shared-Anleitung](Shared/readme_ger.md) beschrieben.

Wenn mehrere Geräte verbunden sind, wähle eines ausdrücklich aus, zum Beispiel:

```bash
ANDROID_SERIAL=emulator-5554 ./scripts/connectedDebugAndroidTest.sh
```

Das vermeidet auch einen Installationskonflikt, falls auf einem anderen Gerät dieselbe Application-ID mit einer anderen Signatur installiert ist. Instrumentierte Tests brauchen ein Gerät oder einen Emulator; Build und lokale JVM-Tests nicht.

Eine einzelne App kannst du mit vollständiger Gradle-Ausgabe erneut prüfen: `./gradlew -p A3_05_SwipeDeleteUndo testDebugUnitTest` oder `ANDROID_SERIAL=emulator-5554 ./gradlew -p A3_05_SwipeDeleteUndo connectedDebugAndroidTest`. Berichte lokaler Tests liegen unter `A3_05_SwipeDeleteUndo/build/reports/tests/testDebugUnitTest/`, Android-Testberichte unter `A3_05_SwipeDeleteUndo/build/reports/androidTests/connected/debug/`.

Die Skripte bauen die Apps nacheinander. Innerhalb eines Builds kann Gradle Aufgaben gemäß `gradle.properties` parallel ausführen. Mehrere App-Builds gleichzeitig sind in dieser Struktur nicht vorgesehen, weil sie die Ausgabeverzeichnisse von `Shared` und `build-logic` gemeinsam nutzen.

## Eine weitere App anlegen und öffnen

Im Repository-Root:

```bash
./scripts/create-app-project.sh A6_01_Sensors
```

Der Name muss `A<Abschnitt>_<zweistellige Nummer>_<Name>` entsprechen, zum Beispiel `A6_01_Sensors`. Ordner und daraus abgeleitete Application-ID müssen noch frei sein. Das Skript erstellt einen Starter mit Manifest und `MainActivity`, kopiert Gradle-Settings und App-Buildskript von `A2_02_Layout` und verlinkt gemeinsame Gradle-Dateien und den Wrapper. Falls im Repository-Root eine `local.properties` vorhanden ist, erhält die neue App einen ignorierten Link darauf. Quellcode anderer Apps wird nicht kopiert, und `Shared` bleibt unverändert.

Prüfe das neue Projekt so:

```bash
cd A6_01_Sensors
./gradlew projects
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

`projects` soll diese App als Root-Projekt und nur `:Shared` als Modul anzeigen. Öffne `MobileSystems/A6_01_Sensors` über **File → Open** in Android Studio und synchronisiere Gradle. Das Öffnen des Repository-Roots lädt nicht alle Apps.

Alle Apps verwenden den gemeinsamen Katalog `gradle/libs.versions.toml` und die Kotlin-Convention-Plugins in `build-logic`: `mobile.android.application` für Apps und `mobile.android.library` für `Shared`. Nach Änderungen daran müssen bereits geöffnete App-Projekte erneut synchronisiert werden. App-spezifische Abhängigkeiten und BuildConfig-Felder gehören in das jeweilige `build.gradle.kts`.

`A5_12_RetrofitNews` liest `NEWS_API_KEY` als Gradle-Property oder aus seiner eigenen `local.properties`; die Datei im Repository-Root ist ein Fallback. Ohne Schlüssel ist `BuildConfig.NEWS_API_KEY` leer. Schlüssel gehören nicht in Git.

Neue Projektdateien und die versionierten Gradle-Links mit Git committen. `local.properties` und Links mit diesem Namen bleiben lokal. Unter Windows müssen Git und Windows symbolische Links für die gemeinsamen Gradle-Dateien und den Wrapper unterstützen.
