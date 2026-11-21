# Shell-Skripte für MobileSystems

[English](readme.md) | Deutsch

Diese Skripte bauen und testen die eigenständigen Android-Apps, erstellen eine weitere App oder exportieren das gesamte Repository als ZIP. Der Projektstamm ist kein gemeinsamer Gradle-Build aller Apps. Die Build- und Testskripte verarbeiten App-Verzeichnisse nach dem Muster `A[0-9]*_*` nacheinander; jede App bindet die Bibliothek `Shared` ein.

## Voraussetzungen und Aufruf

- Bash verwenden, einschließlich Bash 3.2 von macOS, sowie die üblichen Unix-Kommandozeilenwerkzeuge. Skripte direkt oder mit `bash` starten, nicht mit `sh`. Nicht mit `source` einbinden.
- Builds und Tests benötigen den vollständigen Checkout, einen ausführbaren `gradlew` im Projektstamm, ein geeignetes JDK (das Projekt verwendet eine Java-21-Toolchain) und die von Gradle benötigten Android-SDK-Plattformen. Beim ersten Lauf kann Gradle Abhängigkeiten herunterladen.
- `ANDROID_HOME` auf den SDK-Pfad setzen oder passende app-lokale `local.properties` bereitstellen. Die SDK-Konfiguration ist rechnerabhängig.
- Instrumentierte Tests benötigen zusätzlich einen gestarteten Emulator oder ein autorisiertes Android-Gerät.
- Nur der ZIP-Export benötigt zusätzlich **PowerShell ab 7.4** (`pwsh`) und Git im `PATH`.
- Für Windows stehen die [PowerShell-Skripte](../scripts_powershell/readme.md) bereit.

Die folgenden Beispiele starten im Projektstamm:

~~~bash
export ANDROID_HOME="$HOME/Library/Android/sdk" # macOS-Beispiel; ggf. anpassen
./scripts/assembleDebug.sh
# Gleichwertig:
bash ./scripts/assembleDebug.sh
~~~

Jedes Skript ermittelt den Projektstamm aus seinem eigenen Speicherort und funktioniert auch aus einem anderen Arbeitsverzeichnis. Pfade mit Leerzeichen in Anführungszeichen setzen:

~~~bash
"/Pfad mit Leerzeichen/MobileSystems/scripts/assembleDebug.sh"
~~~

## Was die einzelnen Skripte tun

| Skript | Zweck | Geänderte oder erzeugte Dateien |
| --- | --- | --- |
| [assembleDebug.sh](assembleDebug.sh) | Baut die Debug-Version aller Apps. | Entfernt den lokalen Gradle-Konfigurationscache jeder App; Gradle erzeugt Buildausgaben. |
| [testDebugUnitTest.sh](testDebugUnitTest.sh) | Führt lokale JVM-Tests für Shared einmal und danach für jede App aus. | Gradle-Testergebnisse und Berichte. |
| [connectedDebugAndroidTest.sh](connectedDebugAndroidTest.sh) | Führt Geräte-/Emulatortests für Shared einmal und danach für jede App mit Android-Tests aus. | Ersetzt die jeweiligen Connected-Testergebnisse und erzeugt Berichte. |
| [runDebugChecks.sh](runDebugChecks.sh) | Führt Build, lokale Tests und Android-Tests in dieser Reihenfolge aus. | Ausgaben der Einzelschritte und ein Zeitprotokoll unter `logs/`. |
| [create-app-project.sh](create-app-project.sh) | Erstellt eine neue Starter-App aus vorhandenen Gradle-Vorlagen. | Ein neues App-Verzeichnis; vorhandene Apps und Shared bleiben unverändert. |
| [exportZip.sh](exportZip.sh) | Exportiert den aktuellen Dateistand über den gemeinsamen PowerShell-Exporter. | Ein ZIP-Archiv; die Projektquelldateien bleiben unverändert. |

## Bauen und testen

~~~bash
./scripts/assembleDebug.sh
./scripts/testDebugUnitTest.sh
./scripts/connectedDebugAndroidTest.sh

# Alle drei Phasen mit Zeitprotokoll:
./scripts/runDebugChecks.sh

# Alternativ die Einzelskripte ausdrücklich verketten:
./scripts/assembleDebug.sh &&
./scripts/testDebugUnitTest.sh &&
./scripts/connectedDebugAndroidTest.sh
~~~

`assembleDebug.sh` entfernt zunächst `.gradle/configuration-cache` aus allen passenden Apps. Danach ruft es den Gradle-Wrapper des Projektstamms mit `-p <App> assembleDebug` auf. Der globale Gradle-Abhängigkeitscache wird nicht gelöscht.

`testDebugUnitTest.sh` verwendet die Gradle-Settings der ersten App, um `:Shared:testDebugUnitTest` genau einmal auszuführen. Anschließend startet es für jede App `:testDebugUnitTest`. Der führende Doppelpunkt wählt die Task der App aus, ohne die Tests von Shared erneut auszuführen.

`connectedDebugAndroidTest.sh` verwendet dasselbe Shared-einmal-Prinzip mit `:Shared:connectedDebugAndroidTest` und `:connectedDebugAndroidTest`. Ein Modul wird übersprungen, wenn `src/androidTest` keine Kotlin- oder Java-Dateien enthält. Vor dem Testlauf eines Moduls löscht das Skript dessen alte Ergebnisse unter `build/outputs/androidTest-results/connected/debug`. Ein erfolgreicher Gradle-Exit allein genügt nicht: Direkt in diesem Verzeichnis muss außerdem mindestens eine `TEST-*.xml` entstehen. Fehlende Ergebnisse führen zu Exit-Code 1.

Bei mehreren verbundenen Geräten eines ausdrücklich auswählen:

~~~bash
ANDROID_SERIAL=emulator-5554 ./scripts/connectedDebugAndroidTest.sh
# Auch der Koordinator reicht diese Umgebungsvariable an seine Schritte weiter:
ANDROID_SERIAL=emulator-5554 ./scripts/runDebugChecks.sh
~~~

Typische Berichte liegen unter:

- `<App>/build/reports/tests/testDebugUnitTest/index.html`
- `<App>/build/reports/androidTests/connected/debug/index.html`
- Den entsprechenden Verzeichnissen unter `Shared/build/` für die Tests von Shared.

Alle Build-/Testskripte brechen beim ersten Fehler ab. Sie verwenden `--quiet --warning-mode=none`. Zur Fehlersuche die betroffene Task mit normaler Gradle-Ausgabe erneut starten:

~~~bash
./gradlew -p A3_05_SwipeDeleteUndo :testDebugUnitTest
~~~

Mehrere App-Builds oder diese Skripte nicht gleichzeitig ausführen: Die Apps teilen sich die Ausgabeverzeichnisse von Shared und build-logic.

### Zeitprotokoll

`runDebugChecks.sh` startet die drei Skripte in getrennten Bash-Prozessen. Es stoppt bei der ersten fehlgeschlagenen Phase und übernimmt deren Exit-Code. Es zeigt eine Zusammenfassung an und schreibt `logs/debug-checks-<yyyyMMdd-HHmmss>-<PID>.log` mit Start-/Endzeit, Laufzeiten, Status, nicht ausgeführten Phasen und Exit-Code.

Die Logdatei enthält die Startmeldungen der Phasen und die Zeitübersicht, nicht die vollständige Gradle-Ausgabe. Ein EXIT-Trap schreibt die Zusammenfassung bei regulärem Ende einschließlich Fehlern; ein gewaltsames Beenden etwa mit SIGKILL lässt sich nicht abfangen.

## Eine weitere App erstellen

~~~bash
./scripts/create-app-project.sh A6_01_Sensors
~~~

Genau einen Namen nach dem Muster `A[1-9][0-9]*_[0-9][0-9]_[A-Za-z][A-Za-z0-9]*` angeben. Das Verzeichnis darf noch nicht existieren und die abgeleitete Application-ID muss frei sein. Für `A6_01_Sensors` lautet sie `de.rogallab.mobile.a601sensors`.

Das Skript:

1. Prüft Namen, Vorlagendateien, gemeinsame Gradle-Konfiguration und Application-ID-Kollisionen.
2. Kopiert `settings.gradle.kts` und `build.gradle.kts` aus `A2_02_Layout`.
3. Erstellt ein Launcher-Manifest und eine kleine Compose-`MainActivity`, die den App-Namen anzeigt.
4. Erstellt relative symbolische Links auf `gradle/`, `gradle.properties`, `gradlew` und `gradlew.bat` im Projektstamm. Eine vorhandene `local.properties` im Stamm wird ebenfalls verknüpft.
5. Verschiebt das fertige temporäre Verzeichnis an den gewünschten App-Pfad. Bei Fehlern entfernt der EXIT-Trap das temporäre Verzeichnis.

Die App wird nicht automatisch gebaut oder geöffnet. Das neue App-Verzeichnis direkt in Android Studio öffnen und beispielsweise so prüfen:

~~~bash
./gradlew -p A6_01_Sensors projects
./gradlew -p A6_01_Sensors assembleDebug
./gradlew -p A6_01_Sensors :testDebugUnitTest
~~~

Die erzeugten Quell-/Konfigurationsdateien und gemeinsamen Gradle-Links versionieren; `local.properties` bleibt lokal. Ungültige Argumentanzahl oder Namen führen zu Code 2; vorhandene Ziele, fehlende Voraussetzungen oder ID-Kollisionen zu einem Exit-Code ungleich null.

## Das Projekt als ZIP exportieren

~~~bash
./scripts/exportZip.sh
./scripts/exportZip.sh "/tmp/MobileSystems Kurs.zip"
./scripts/exportZip.sh --destination "/tmp/MobileSystems Kurs.zip" --force
./scripts/exportZip.sh --help
~~~

Dieser Bash-Einstieg ruft [exportZip.ps1](../scripts_powershell/exportZip.ps1) auf. Er benötigt `pwsh` und implementiert keinen eigenen ZIP-Algorithmus.

- Das Archiv enthält den übergeordneten Ordner `MobileSystems/` mit allen berücksichtigten Apps, Shared, build-logic, Gradle-Konfigurationen und Wrappern.
- Ohne Zielangabe entsteht `MobileSystems-yyyyMMdd-HHmmss-fff.zip` im Projektstamm.
- Relative Ziele beziehen sich auf das Arbeitsverzeichnis des Aufrufers. Das übergeordnete Zielverzeichnis muss existieren.
- Bestehende Archive werden nur mit ausdrücklichem `--force` ersetzt (auch `-Force` möglich). `--destination` akzeptiert auch die Schreibweise `-Destination`.
- Der Exporter verwendet gespeicherte aktuelle Dateien einschließlich neuer, unversionierter Dateien und Git zur Auswertung verschachtelter `.gitignore`-Regeln und Ausnahmen. Globale Git-Ausschlüsse und `.git/info/exclude` gelten ebenfalls.
- Zusätzliche Ausschlüsse entfernen Buildausgaben, Caches, Logs, temporäre Dateien, lokale SDK-Einstellungen, IDE-Metadaten, `.git` und sämtliche ZIP-Dateien. Wrapper-JARs bleiben trotz Regeln wie `*.jar` erhalten.
- Interne symbolische Links werden als normale Dateien bzw. Verzeichnisinhalte archiviert. Externe, defekte oder zyklische Links führen zum Fehler.
- Die Quelle muss ein Git-Arbeitsverzeichnis mit dem Projektverzeichnis als Stamm sein. Der Export ist kein atomarer Snapshot; währenddessen keine Dateien bearbeiten oder Builds starten.

Die vollständigen Ausschlüsse und Einschränkungen stehen in der [ausführlichen Export-Dokumentation mit Prüfbericht](../scripts_powershell/readme.md#projekt-als-zip-exportieren). Nach dem Entpacken lokale SDK-Einstellungen und ignorierte Zugangsdaten neu einrichten; je nach Entpacker die Ausführungsrechte von `gradlew` wiederherstellen.

Erfolg liefert Code 0. Exportfehler und fehlendes `pwsh` liefern Code 1, ungültige Shell-Argumente Code 2. Der Bash-Einstieg reicht den Exit-Code des PowerShell-Exporters weiter.
