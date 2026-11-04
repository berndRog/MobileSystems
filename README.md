# MobileSystems

Dieses Git-Repository enthält eigenständige Android-App-Projekte (`A2_*`, `A3_*`, `A4_*`, `A5_*` und weitere Abschnitte) und die einmal vorhandene Android-Library `Shared`. Öffne in Android Studio **einen App-Ordner direkt**, zum Beispiel `A5_10_PeopleRetrofit`. Dessen Gradle-Projekt lädt nur diese App und `Shared`. Der Repository-Root ist kein Sammelprojekt für die Apps.

## Neue App anlegen

Im Repository-Root ausführen:

```bash
./scripts/create-app-project.sh A6_01_Sensors
```

Der Name muss dem Muster `A<Abschnitt>_<zweistellige Nummer>_<Name>` entsprechen, zum Beispiel `A6_01_Sensors`. Der Abschnitt kann auch größer als 5 sein. Der Ordner und die daraus abgeleitete Application-ID dürfen noch nicht existieren. Das Skript legt einen direkt baubaren Starter mit Manifest und `MainActivity` an. Es kopiert die Gradle-Settings und das App-Buildskript von `A2_02_Layout` und erstellt Links auf den gemeinsamen Gradle-Ordner, Wrapper und `gradle.properties`. Falls im Repository-Root eine lokale `local.properties` vorhanden ist, wird auch diese für den Android-SDK-Pfad verlinkt; der Link wird von Git ignoriert. Es kopiert **keinen** Code aus anderen Apps und verändert `Shared` nicht.

Alle Apps verwenden dadurch dieselbe `gradle/libs.versions.toml` und die Kotlin-Convention-Plugins in `build-logic`. Das Plugin `mobile.android.application` konfiguriert die App, `mobile.android.library` konfiguriert `Shared`. Nach Änderungen am Katalog oder an `build-logic` muss jedes bereits geöffnete App-Projekt in Android Studio erneut synchronisiert werden. App-spezifische Abhängigkeiten und BuildConfig-Felder gehören in das jeweilige `build.gradle.kts`; `A5_12_RetrofitNews` zeigt ein Beispiel für ein app-spezifisches Feld. Gruppen von Abhängigkeiten sind dort und in den Convention-Plugins mit Dokumentationslinks kommentiert.

Für `A5_12_RetrofitNews` kann `NEWS_API_KEY` als Gradle-Property oder in `A5_12_RetrofitNews/local.properties` gesetzt werden. Als Fallback wird `local.properties` im Repository-Root gelesen. Die App verwendet den Wert als `BuildConfig.NEWS_API_KEY`.

## Prüfen und öffnen

```bash
cd A6_01_Sensors
./gradlew projects
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

`projects` soll nur die neue App als Root-Projekt und `:Shared` anzeigen. Öffne anschließend über **File → Open** den Ordner `MobileSystems/A6_01_Sensors` in Android Studio und führe einen Gradle-Sync aus. Zum Debuggen muss dieses App-Projekt geöffnet sein; das bloße Öffnen der `settings.gradle.kts` im Repository-Root lädt es nicht als Gradle-Projekt.

Gradle liest `local.properties` aus dem jeweiligen App-Ordner. Der Eintrag im Repository-Root gilt daher nicht automatisch für ältere Apps ohne eigene Datei. Setze für einen Lauf über alle Apps `ANDROID_HOME` auf den SDK-Pfad (auf diesem Mac: `$HOME/Library/Android/sdk`). Führe die drei Skripte im Repository-Root nacheinander aus:

```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
./scripts/assemble.sh && \
./scripts/test.sh && \
./scripts/androidtest.sh
```

`assemble.sh` löscht zuerst den lokalen Configuration Cache jeder App und baut sie dann mit `assembleDebug`. Jedes Skript gibt vor seiner Gradle-Aufgabe den App-Ordner aus und stoppt beim ersten Fehler. Durch `&&` startet das nächste Skript nur nach erfolgreichem Abschluss des vorherigen. Der Configuration Cache wird beim Build neu erstellt; der globale Gradle-Cache mit heruntergeladenen Abhängigkeiten bleibt erhalten. Für `androidtest.sh` muss ein Gerät oder Emulator verbunden sein. Das Skript überspringt Apps ohne Android-Tests und meldet fehlende Testergebnisse als Fehler, auch wenn Gradle mit Exit-Code 0 endet.

Sind mehrere Geräte verbunden, wähle das Testgerät ausdrücklich aus, zum Beispiel mit `ANDROID_SERIAL=emulator-5554 ./scripts/androidtest.sh`. Das ist auch sinnvoll, wenn auf einem anderen Gerät eine App mit derselben Application-ID, aber einer anderen Signatur installiert ist.

Die Skripte verwenden `--quiet --warning-mode=none`: Gradle zeigt dadurch weder die normalen Cache- und Report-Hinweise noch die Zusammenfassung veralteter Funktionen an. Fehler bleiben sichtbar; nach erfolgreichem Abschluss meldet das Skript `OK` für die jeweilige App. Für die Diagnose einer App können die beiden Optionen vorübergehend aus dem betreffenden Skript entfernt werden.

Alternativ können App-Ordner ohne eigene `local.properties` einen symbolischen Link auf `../local.properties` erhalten. Dann funktionieren die Gradle-Aufrufe auch ohne `ANDROID_HOME`. Diese lokalen Dateien und Links sind durch `.gitignore` ausgeschlossen. `gradle.properties` ist nicht der Ort für `sdk.dir`.

Die neuen Dateien und symbolischen Links mit Git hinzufügen und committen. Auf Windows müssen symbolische Links in der lokalen Git-/Windows-Konfiguration unterstützt werden.
