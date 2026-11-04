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

## Alle Apps bauen und testen

Führe die drei Skripte im Repository-Root der Reihe nach aus. Jedes Skript lässt sich auch einzeln oder aus einem anderen Arbeitsverzeichnis starten.

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk" # an deinen Rechner anpassen
./scripts/assemble.sh && \
./scripts/test.sh && \
./scripts/androidtest.sh
```

1. `assemble.sh` löscht den lokalen Gradle-Configuration-Cache jeder App, gibt ihren Namen aus und führt `assembleDebug` aus.
2. `test.sh` gibt den App-Namen aus und führt die lokalen JVM-Tests mit `testDebugUnitTest` aus.
3. `androidtest.sh` gibt den App-Namen aus und führt `connectedDebugAndroidTest` auf einem verbundenen Gerät oder Emulator aus. Apps ohne Android-Tests werden als übersprungen markiert. Fehlen bei einer App mit Android-Tests die Testergebnisse, meldet das Skript auch dann einen Fehler, wenn Gradle mit Exit-Code 0 endet.

Jedes Skript stoppt beim ersten Fehler. Durch `&&` beginnt die nächste Phase nur nach erfolgreichem Abschluss der vorherigen. Gelöscht wird nur der Configuration Cache der Apps; der globale Gradle-Cache mit heruntergeladenen Abhängigkeiten bleibt erhalten. Die Skripte verwenden `--quiet --warning-mode=none`: Übliche Gradle-Meldungen werden ausgeblendet, Fehler bleiben sichtbar. Zur Diagnose kannst du die Gradle-Aufgabe einer App ohne diese Optionen starten.

Wenn mehrere Geräte verbunden sind, wähle eines ausdrücklich aus, zum Beispiel:

```bash
ANDROID_SERIAL=emulator-5554 ./scripts/androidtest.sh
```

Das vermeidet auch einen Installationskonflikt, falls auf einem anderen Gerät dieselbe Application-ID mit einer anderen Signatur installiert ist. Instrumentierte Tests brauchen ein Gerät oder einen Emulator; Build und lokale JVM-Tests nicht.

Eine einzelne App kannst du mit vollständiger Gradle-Ausgabe erneut prüfen: `./gradlew -p A3_05_SwipeDeleteUndo testDebugUnitTest` oder `ANDROID_SERIAL=emulator-5554 ./gradlew -p A3_05_SwipeDeleteUndo connectedDebugAndroidTest`. Berichte lokaler Tests liegen unter `A3_05_SwipeDeleteUndo/build/reports/tests/testDebugUnitTest/`, Android-Testberichte unter `A3_05_SwipeDeleteUndo/build/reports/androidTests/connected/debug/`.

Die Skripte bauen die Apps nacheinander. Innerhalb eines Builds kann Gradle Aufgaben gemäß `gradle.properties` parallel ausführen. Mehrere App-Builds gleichzeitig sind in dieser Struktur nicht vorgesehen, weil sie die Ausgabeverzeichnisse von `Shared` und `build-logic` gemeinsam nutzen.

## Teststruktur und Lernprogression

Die Tests verwenden die üblichen Android-Source-Sets: `src/test/java` für lokale JVM-Tests und `src/androidTest/java` für Tests, die Android oder Compose auf einem Gerät benötigen. Die Test-Packages spiegeln die Production-Packages. Modullokale Hilfen liegen unter `de.rogallab.mobile.testing`. Lokale Tests prüfen unter anderem Mapping, Validatoren, ViewModels, Effects und Repository-Fakes; instrumentierte Tests prüfen Verhalten wie Swipe-Gesten.

- `A3_01_Material`: Mapping, Validation und grundlegender ViewModel-State.
- `A3_02_EffectHandling`: einmalige `ShowMessage`-/`ShowError`-Effects und vorbereitete Back-Navigation.
- `A3_03_Navigation`: `NavigateTo`-/`NavigateBack`-Effects und Navigation 3. Die App nutzt Material 3 `SnackbarHostState` direkt; ein eigener Snackbar-Coordinator-Test entfällt.
- `A3_04_SwipeGestures`: StartToEnd öffnet eine bestehende Person im Detailmodus. EndToStart fordert über eine Action-Snackbar eine Bestätigung an; erst deren Delete-Action ändert das Repository. Instrumentierte Tests prüfen beide Richtungen. Undo folgt im nächsten Schritt.
- `A3_05_SwipeDeleteUndo`: ergänzt sofortiges visuelles Entfernen, Undo, verzögerten Repository-Commit, Wiederherstellung bei Commit-Fehlern und das Sichtbarmachen eines wiederhergestellten Eintrags. Die Tests prüfen Restore und Undo.
- `A4_01_ImagePicker`: ergänzt Bildauswahl, Bildpfad-State und den Lebenszyklus temporärer oder ersetzter Bilder, einschließlich erfolgreichem und fehlgeschlagenem Speichern.
- `Shared`: testet wiederverwendbares Verhalten von `SnackbarController`, `EffectDelegate` und `SwipeCard`. `SwipeCard` erkennt Gesten; die Apps liefern den Karteninhalt. Lokale Snackbar-Tests prüfen direkte Message-/Error-Aufrufe und die Ergebnisse `performAction`/`dismiss`.

Die Apps verwenden `targetSdk = 37`. Android-nahe lokale Tests mit Robolectric 4.16.1 wählen über `@Config(sdk = [35])` die unterstützte Test-API 35; das ändert die Produktionskonfiguration nicht. `MainDispatcherRule` schaltet `Alog` auf Nicht-Android-Ausgabe, damit reine JVM-Tests nicht `android.util.Log` aufrufen.

Tests müssen nicht direkt neben den Production-Dateien liegen, aber einem Gradle-Test-Source-Set zugeordnet sein. Größere Projekte können Gradle `testFixtures` oder ein separates Test-Support-Modul verwenden. In diesen Lehrprojekten bleiben die wenigen Hilfsklassen in den einzelnen Modulen.

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
