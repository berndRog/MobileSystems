# MobileSystems

Dieses Git-Repository enthält eigenständige Android-App-Projekte (`A2_*`, `A3_*`, `A4_*`, `A5_*` und weitere Abschnitte) und die einmal vorhandene Android-Library `Shared`. Öffne in Android Studio **einen App-Ordner direkt**, zum Beispiel `A5_10_PeopleRetrofit`. Dessen Gradle-Projekt lädt nur diese App und `Shared`. Der Repository-Root ist kein Sammelprojekt für die Apps.

## Neue App anlegen

Im Repository-Root ausführen:

```bash
./scripts/create-app-project.sh A6_01_Sensors
```

Der Name muss dem Muster `A<Abschnitt>_<zweistellige Nummer>_<Name>` entsprechen, zum Beispiel `A6_01_Sensors`. Der Abschnitt kann auch größer als 5 sein. Der Ordner und die daraus abgeleitete Application-ID dürfen noch nicht existieren. Das Skript legt einen direkt baubaren Starter mit Manifest und `MainActivity` an. Es kopiert die Gradle-Settings und das App-Buildskript von `A2_02_Layout` und erstellt Links auf den gemeinsamen Gradle-Ordner, Wrapper und `gradle.properties`. Falls im Repository-Root eine lokale `local.properties` vorhanden ist, wird auch diese für den Android-SDK-Pfad verlinkt; der Link wird von Git ignoriert. Es kopiert **keinen** Code aus anderen Apps und verändert `Shared` nicht.

Alle Apps verwenden dadurch dieselbe `gradle/libs.versions.toml` und `gradle/android-common.gradle`. Nach Änderungen daran muss jedes bereits geöffnete App-Projekt in Android Studio erneut synchronisiert werden. App-spezifische Abhängigkeiten und BuildConfig-Felder gehören in das jeweilige `build.gradle.kts`; `A5_12_RetrofitNews` zeigt ein Beispiel für ein app-spezifisches Feld.

## Prüfen und öffnen

```bash
cd A6_01_Sensors
./gradlew projects
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

`projects` soll nur die neue App als Root-Projekt und `:Shared` anzeigen. Öffne anschließend über **File → Open** den Ordner `MobileSystems/A6_01_Sensors` in Android Studio und führe einen Gradle-Sync aus. Zum Debuggen muss dieses App-Projekt geöffnet sein; das bloße Öffnen der `settings.gradle.kts` im Repository-Root lädt es nicht als Gradle-Projekt.

Fehlt `local.properties` im Repository-Root, trage den Android-SDK-Pfad im neuen Projekt ein oder setze `ANDROID_HOME`, bevor du die Gradle-Befehle ausführst.

Die neuen Dateien und symbolischen Links mit Git hinzufügen und committen. Auf Windows müssen symbolische Links in der lokalen Git-/Windows-Konfiguration unterstützt werden.
