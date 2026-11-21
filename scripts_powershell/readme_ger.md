# PowerShell-Skripte für MobileSystems

[English](readme.md) | Deutsch

Diese Skripte bauen und testen die eigenständigen Android-Apps, erstellen eine neue App oder exportieren das gesamte Repository als ZIP. Der Projektstamm ist kein gemeinsamer Gradle-Build aller Apps. Die Build-/Testskripte verarbeiten die App-Verzeichnisse in sortierter Reihenfolge; jede App bindet `Shared` ein.

## Voraussetzungen und Aufruf

- **PowerShell 7** (`pwsh`), nicht Windows PowerShell 5.1. Der ZIP-Export benötigt **mindestens Version 7.4** und Git im `PATH`.
- Vollständiger Checkout einschließlich `gradlew.bat`, `gradle/`, `build-logic/` und `Shared/`.
- Für Builds und Tests: ein passendes JDK (das Projekt verwendet eine Java-21-Toolchain), Android SDK und die laut Gradle benötigten Plattformen. Beim ersten Lauf kann Gradle Abhängigkeiten herunterladen.
- SDK-Pfad über `ANDROID_HOME` oder passende app-lokale `local.properties` bereitstellen.
- Für instrumentierte Tests: gestarteter Emulator oder autorisiertes Android-Gerät.
- Nur für die Projektgenerierung unter Windows: Entwicklermodus oder Berechtigung zum Erstellen symbolischer Links. Ohne diese Rechte bricht das Skript ab und entfernt den temporären Projektordner.
- Die lokale Ausführungsrichtlinie muss die geprüften Skripte zulassen. Die Skripte ändern keine systemweite Execution Policy.

Unter Windows werden Bash, WSL, Git Bash und Unix-Werkzeuge nicht benötigt. Unter macOS/Linux verwenden die Build-/Testskripte den vorhandenen ausführbaren Unix-Wrapper `gradlew`, unter Windows `gradlew.bat`. Die [Bash-Anleitungen](../scripts/readme_ger.md) erklären die Shell-Varianten.

Beispiele aus dem Projektstamm, ausgeführt in PowerShell:

~~~powershell
# SDK-Pfad an den eigenen Rechner anpassen
$env:ANDROID_HOME = "C:\Users\Name\AppData\Local\Android\Sdk"
pwsh -NoProfile -File .\scripts_powershell\assembleDebug.ps1
~~~

Alle Skripte ermitteln ihren Projektstamm über `$PSScriptRoot` und funktionieren aus einem fremden Arbeitsverzeichnis. Pfade mit Leerzeichen in Anführungszeichen setzen:

~~~powershell
pwsh -NoProfile -File "C:\Android Projekte\MobileSystems\scripts_powershell\runDebugChecks.ps1"
# Innerhalb einer PowerShell-Sitzung:
& "C:\Android Projekte\MobileSystems\scripts_powershell\create-app-project.ps1" A6_01_Sensors
$LASTEXITCODE
~~~

Skripte ausführen, nicht dot-sourcen. Unter macOS/Linux sind die fünf Build-/Test-/Generatorskripte dank Shebang und Ausführungsrecht auch direkt startbar, sofern `pwsh` im `PATH` liegt:

~~~sh
./scripts_powershell/assembleDebug.ps1
./scripts_powershell/create-app-project.ps1 A6_01_Sensors
# Expliziter PowerShell-Aufruf funktioniert ebenfalls:
pwsh -NoProfile -File ./scripts_powershell/exportZip.ps1
~~~

## Was die einzelnen Skripte tun

| Skript | Zweck | Geänderte oder erzeugte Dateien |
| --- | --- | --- |
| [assembleDebug.ps1](assembleDebug.ps1) | Baut die Debug-Version aller Apps. | Entfernt zunächst die lokalen Gradle-Konfigurationscaches aller Apps; Gradle erzeugt Buildausgaben. |
| [testDebugUnitTest.ps1](testDebugUnitTest.ps1) | Führt lokale JVM-Tests für Shared einmal und danach für jede App aus. | Gradle-Testergebnisse und Berichte. |
| [connectedDebugAndroidTest.ps1](connectedDebugAndroidTest.ps1) | Führt Geräte-/Emulatortests für Shared einmal und danach für Apps mit Android-Tests aus. | Ersetzt die jeweiligen Connected-Testergebnisse und erzeugt Berichte. |
| [runDebugChecks.ps1](runDebugChecks.ps1) | Führt Build, lokale Tests und Android-Tests in dieser Reihenfolge aus. | Ausgaben der Einzelschritte und ein Zeitprotokoll unter `logs/`. |
| [create-app-project.ps1](create-app-project.ps1) | Erstellt eine Starter-App aus vorhandenen Gradle-Vorlagen. | Ein neues App-Verzeichnis; vorhandene Apps und Shared bleiben unverändert. |
| [exportZip.ps1](exportZip.ps1) | Exportiert den aktuellen gespeicherten Dateistand des Projekts als ZIP. | Ein Archiv; Projektquelldateien bleiben unverändert. |

## Bauen und testen

~~~powershell
pwsh -NoProfile -File .\scripts_powershell\assembleDebug.ps1
pwsh -NoProfile -File .\scripts_powershell\testDebugUnitTest.ps1
pwsh -NoProfile -File .\scripts_powershell\connectedDebugAndroidTest.ps1

# Alle drei Phasen mit Abbruch beim ersten Fehler und Zeitprotokoll:
pwsh -NoProfile -File .\scripts_powershell\runDebugChecks.ps1
$LASTEXITCODE
~~~

Die drei Einzelaufrufe sind unabhängig. Für eine automatisch beim ersten Fehler stoppende Gesamtfolge `runDebugChecks.ps1` verwenden.

`assembleDebug.ps1` entfernt zuerst nur `.gradle/configuration-cache` aus allen passenden Apps und ruft anschließend den Wrapper mit `-p <App> assembleDebug` auf. Der globale Gradle-Abhängigkeitscache bleibt erhalten.

`testDebugUnitTest.ps1` führt über die Settings der ersten App `:Shared:testDebugUnitTest` genau einmal aus. Danach läuft `:testDebugUnitTest` für jede App. Der führende Doppelpunkt wählt nur die App-Task, ohne die Tests von Shared erneut auszuführen.

`connectedDebugAndroidTest.ps1` verwendet dasselbe Prinzip mit `:Shared:connectedDebugAndroidTest` und `:connectedDebugAndroidTest`. Module ohne Kotlin-/Java-Dateien unter `src/androidTest` werden übersprungen. Vor dem Testlauf werden alte Ergebnisse unter `build/outputs/androidTest-results/connected/debug` entfernt. Nach erfolgreichem Gradle-Lauf muss mindestens eine `TEST-*.xml` direkt in diesem Ordner liegen; sonst endet das Skript mit einem Fehler.

Bei mehreren Geräten eines ausdrücklich auswählen. Die Umgebungsvariable gilt in der aktuellen PowerShell-Sitzung bis zum Entfernen und wird an die Kindprozesse weitergereicht:

~~~powershell
$env:ANDROID_SERIAL = "emulator-5554"
pwsh -NoProfile -File .\scripts_powershell\connectedDebugAndroidTest.ps1
# Alternativ die vollständige Folge:
pwsh -NoProfile -File .\scripts_powershell\runDebugChecks.ps1
Remove-Item Env:ANDROID_SERIAL
~~~

Typische Berichte liegen unter `<App>/build/reports/tests/testDebugUnitTest/index.html` und `<App>/build/reports/androidTests/connected/debug/index.html`; für Shared entsprechend unter `Shared/build/`.

Die Skripte brechen beim ersten Fehler ab und verwenden `--quiet --warning-mode=none`. Zur Diagnose eine einzelne Task mit normaler Gradle-Ausgabe starten:

~~~powershell
# Windows
.\gradlew.bat -p A3_05_SwipeDeleteUndo :testDebugUnitTest
# macOS/Linux in PowerShell
./gradlew -p A3_05_SwipeDeleteUndo :testDebugUnitTest
~~~

Mehrere App-Builds oder diese Skripte nicht gleichzeitig ausführen: Shared und build-logic verwenden gemeinsame Ausgabeverzeichnisse.

### Zeitprotokoll und Fehlercodes

`runDebugChecks.ps1` startet die drei Schritte in getrennten PowerShell-Prozessen und stoppt beim ersten Fehler. Ein `finally`-Block schreibt Start-/Endzeiten, Laufzeiten, Status, nicht ausgeführte Phasen und Exit-Code nach `logs/debug-checks-<Zeitstempel>-<PID>.log` und auf die Konsole. Die Zeitzone wird als UTC-Offset angegeben.

Die Datei enthält Startmeldungen und Zusammenfassung, nicht die vollständige Gradle-Ausgabe. Bei regulärem Ctrl+C wird die Zusammenfassung bestmöglich geschrieben; gewaltsames Beenden kann nicht abgefangen werden.

Native Exit-Codes werden ausdrücklich geprüft und weitergereicht. Erfolg liefert 0, eigene Fehler normalerweise 1; ungültige Generatorargumente liefern 2. Den Code eines Aufrufs mit `$LASTEXITCODE` prüfen.

## Eine weitere App erstellen

~~~powershell
pwsh -NoProfile -File .\scripts_powershell\create-app-project.ps1 A6_01_Sensors
~~~

Genau ein positionales Argument nach dem Muster `A[1-9][0-9]*_[0-9][0-9]_[A-Za-z][A-Za-z0-9]*` angeben. Das Verzeichnis und die abgeleitete Application-ID müssen frei sein. Für `A6_01_Sensors` lautet die ID `de.rogallab.mobile.a601sensors`.

Der Generator prüft Voraussetzungen und Kollisionen, kopiert `settings.gradle.kts` und `build.gradle.kts` unverändert aus `A2_02_Layout` und erstellt ein Launcher-Manifest sowie eine kleine Compose-`MainActivity` mit dem App-Namen. Neue Textdateien werden als UTF-8 ohne BOM mit LF geschrieben; die Vorlagen bleiben bytegetreu.

`gradle/`, `gradle.properties`, `gradlew` und `gradlew.bat` werden symbolisch mit dem Projektstamm verknüpft; eine vorhandene `local.properties` ebenfalls. Anders als der Bash-Generator verwendet diese Variante aus Kompatibilität mit PowerShell 7.0 **absolute Linkziele**. Nach einem Umzug des Checkouts müssen diese Links angepasst werden. Es gibt keinen stillen Fallback auf separate Dateikopien.

Die Erstellung erfolgt zunächst in einem temporären Geschwisterordner. Fehler und regulärer Abbruch räumen ihn auf; ein vorhandenes Ziel wird nicht überschrieben. Die App wird nicht automatisch gebaut oder geöffnet. Das neue App-Verzeichnis direkt in Android Studio öffnen und prüfen:

~~~powershell
# Windows; unter macOS/Linux stattdessen ./gradlew verwenden
.\gradlew.bat -p A6_01_Sensors projects
.\gradlew.bat -p A6_01_Sensors assembleDebug
.\gradlew.bat -p A6_01_Sensors :testDebugUnitTest
~~~

Quell-/Konfigurationsdateien und Gradle-Links versionieren; `local.properties` bleibt lokal.

## Projekt als ZIP exportieren

Als Ersatz für Android Studios „Export ZIP“ exportiert `exportZip.ps1` den gesamten Projektstamm mit allen Apps, `Shared`, `build-logic` und Gradle-Wrappern unter dem Archivordner `MobileSystems/`.

Voraussetzungen: **PowerShell 7.4 oder neuer**, Git im `PATH` und ein Git-Arbeitsverzeichnis, dessen Stamm dem Skript-Projektstamm entspricht. Java, Android SDK und externe ZIP-Werkzeuge werden für den Export nicht benötigt.

~~~powershell
# Windows: Zeitstempelname im Projektstamm
pwsh -NoProfile -File .\scripts_powershell\exportZip.ps1

# Eigener ZIP-Dateipfad; das übergeordnete Verzeichnis muss existieren
pwsh -NoProfile -File .\scripts_powershell\exportZip.ps1 -Destination "C:\Backups\MobileSystems Kurs.zip"

# Ein vorhandenes Archiv ausdrücklich zum Überschreiben freigeben
pwsh -NoProfile -File .\scripts_powershell\exportZip.ps1 "C:\Backups\MobileSystems Kurs.zip" -Force
~~~

~~~sh
# macOS, auch aus einem beliebigen Arbeitsverzeichnis
pwsh -NoProfile -File "/Pfad mit Leerzeichen/MobileSystems/scripts_powershell/exportZip.ps1" \
  -Destination "/tmp/MobileSystems Kurs.zip"
~~~

Für macOS/Linux gibt es außerdem den ausführbaren Bash-Einstieg `scripts/exportZip.sh`. Er ruft dieselbe PowerShell-Implementierung auf und benötigt ebenfalls `pwsh` ab Version 7.4 sowie Git. Die Exportlogik wird nur einmal gepflegt.

~~~sh
./scripts/exportZip.sh
./scripts/exportZip.sh "/tmp/MobileSystems Kurs.zip"
./scripts/exportZip.sh --destination "/tmp/MobileSystems Kurs.zip" --force
# Auch aus einem fremden Arbeitsverzeichnis:
"/Pfad mit Leerzeichen/MobileSystems/scripts/exportZip.sh" "./Mein Export.zip"
./scripts/exportZip.sh --help
~~~

Der Shell-Einstieg unterstützt `--force` (auch `-Force`) und `--destination` (auch `-Destination`). Er reicht den Exit-Code des Exporters weiter; ungültige Shell-Argumente führen zu Code 2, fehlendes `pwsh` zu Code 1.

Der Stamm wird aus `$PSScriptRoot` ermittelt. Relative Zielpfade beziehen sich auf das aktuelle Arbeitsverzeichnis des Aufrufers. Ohne Zielargument entsteht `MobileSystems-yyyyMMdd-HHmmss-fff.zip` im Projektstamm. Bestehende Archive bleiben ohne `-Force` unverändert; es gibt keine interaktive Rückfrage. Das Archiv wird zunächst als temporäre Datei im Zielverzeichnis erstellt und erst nach erfolgreichem Abschluss verschoben. Fehler werden auf stderr mit Exit-Code 1 gemeldet, Erfolg mit 0.

Die Auswahl basiert auf dem aktuellen Dateisystem, einschließlich geänderter und neuer, noch nicht versionierter Dateien. Git dient ausschließlich als Ignore-Engine (`git check-ignore --no-index -z --stdin`), nicht als Quelle eines Commits oder des Index. Dadurch gelten auch für bereits versionierte Dateien die aktuellen Ausschlüsse. Verschachtelte `.gitignore`-Dateien und `!`-Ausnahmen verarbeitet Git selbst, einschließlich seiner Regel, dass Dateien unter weiterhin ausgeschlossenen Elternverzeichnissen nicht wieder eingeschlossen werden können. Auch `.git/info/exclude` und globale Git-Ausschlüsse gelten; dadurch kann sich die Auswahl zwischen Rechnern unterscheiden.

Zusätzliche feste Ausschlüsse haben Vorrang vor Git-Ausnahmen:

- Verzeichnisse `.git`, `.gradle`, `.kotlin`, `.idea`, `build`, `out`, `log`/`logs`, `tmp`/`temp`, `.cache`/`caches`, `captures`, `.cxx` und `.externalNativeBuild`, auf jeder Ebene.
- `local.properties` (auch als Link), `.DS_Store` und `Thumbs.db`.
- Dateien mit Endungen `.zip`, `.apk`, `.aab`, `.class`, `.dex`, `.hprof`, `.log`, `.tmp`, `.temp`, `.swp`, `.swo`, `.bak`, `.iml`, `.ipr`, `.iws` und Namen mit abschließendem `~`. Damit werden auch alle bisherigen ZIP-Exporte ausgeschlossen, selbst wenn sie anders heißen.
- Die vorhandenen Git-Regeln schließen zusätzlich beispielsweise Signierschlüssel aus.

Gezielte Ausnahme: Dateien am Pfad `gradle/wrapper/gradle-wrapper.jar` (auch unter Apps) bleiben trotz Git-Regeln wie `*.jar` enthalten. Wichtige gemeinsame Gradle-Dateien werden vor dem Schreiben auf Vorhandensein in der Auswahl geprüft; fehlen sie oder werden sie ignoriert, bricht der Export verständlich ab.

Interne symbolische Links werden als normale Dateien bzw. Verzeichnisinhalte archiviert. Bei Verzeichnislinks prüft Git die Linkpfade und die tatsächlichen Quellpfade mit deren jeweiligen Ignore-Regeln, da Git Pfade unterhalb eines symbolischen Links nicht direkt prüfen kann. Das macht die entpackte Kopie unabhängig von Linkrechten und alten absoluten Linkzielen. Externe, defekte oder zyklische Links führen zum Fehler. Unix-Ausführungsrechte werden im ZIP hinterlegt; ob ein Entpacker sie wiederherstellt, hängt vom Werkzeug ab (gegebenenfalls `chmod +x gradlew`). Leere Verzeichnisse werden nicht gespeichert. Die entpackte Kopie hat keine Git-Metadaten; ein weiterer Export benötigt dort zuerst ein eigenes Git-Arbeitsverzeichnis.

Während des Exports sollten keine Builds oder Dateiänderungen laufen: Der Export ist kein atomarer Snapshot. Es werden gespeicherte Dateien gelesen, keine ungespeicherten Editorpuffer. Lokale SDK-Pfade und ignorierte Zugangsdaten müssen nach dem Entpacken neu eingerichtet werden. Ein ZIP-Inhaltstest ersetzt keinen Gradle-Build.

## Validierung

Die folgenden Prüfberichte wurden aus der bisherigen Dokumentation übernommen. Bei dieser Dokumentationsübertragung wurden keine Builds oder Funktionstests erneut ausgeführt.

Nach der Umbenennung in `exportZip.ps1` und Ergänzung von `scripts/exportZip.sh` auf macOS mit Bash 3.2 und PowerShell 7.6.9 erneut geprüft: Beide Aufrufe erzeugten Archive mit 1.767 Einträgen und bytegleichen Dateiinhalten. Die Archive wurden entpackt und die 17 App-Verzeichnisse geprüft. Zusätzlich wurden Standardziel, fremdes Arbeitsverzeichnis, Checkout- und Zielpfade mit Leerzeichen, Unicode im Zielnamen, Überschreibschutz, `--force`, fehlerhafte Argumente, Hilfe und fehlendes `pwsh` geprüft. Insgesamt 3.554 Prüfungen erfolgreich; `bash -n` und `git diff --check` ebenfalls ohne Fehler. Weiterhin keine Windows-Ausführung durchgeführt.

ZIP-Export: Auf macOS mit PowerShell 7.6.9 erfolgreich ausgeführt. Das echte Archiv enthielt 1.766 Dateien und wurde in ein temporäres Verzeichnis mit Leerzeichen entpackt. 308 Prüfungen bestätigten unter anderem alle 17 App-Strukturen mit Manifesten, Gradle-Dateien und materialisierten Wrappern, bytegleiche Inhalte einschließlich einer bereits lokal geänderten Quelldatei und des neuen unversionierten Skripts, Shared und build-logic sowie fehlende Buildartefakte, Caches, Logs, ZIPs und lokale SDK-Konfigurationen. In der entpackten Testkopie wurden zusätzlich verschachtelte Ignore-Regeln und Ausnahmen, bereits versionierte ignorierte Dateien, neue Unicode-/Leerzeichen-Dateinamen, Wrapper-JARs trotz `*.jar`, Standardname, fremdes Arbeitsverzeichnis, Überschreibschutz, `-Force`, Fehlercodes, Erhalt eines alten Archivs bei fehlgeschlagenem Export und interne/externe/defekte/zyklische Links geprüft. Auch macOS-Pfadaliase (`/var` und `/private/var`) wurden getestet. Kein Gradle-Build der entpackten Kopie und keine Windows-Ausführung durchgeführt. Windows-Dateisystem- und Entpackerverhalten bleiben dort zu prüfen.

Durchgeführt auf macOS mit PowerShell 7.4.6 über .NET: Syntaxprüfung aller fünf Skripte mit dem PowerShell-Parser und 54 kontrollierte Funktionstests in temporären Projekten mit Leerzeichen im Pfad. Gradle wurde dabei durch ein Testdouble ersetzt; für den über .NET gestarteten Testhost wurde nur die Host-Ermittlung des Koordinators gemockt. Geprüft wurden unter anderem Reihenfolge, Shared genau einmal, Fehlercodes, fehlende/veraltete/verschachtelte Berichte, Zeitprotokolle, Generatorargumente, ID-Kollisionen und Aufräumen bei fehlenden Linkrechten. Manifest, Activity und beide Gradle-Dateien wurden zusätzlich bytegenau mit den Ergebnissen des Bash-Originals verglichen. UTF-8 ohne BOM, LF, optionale lokale SDK-Konfiguration und gemeinsam genutzte Links wurden geprüft.

Nach Ergänzung der betriebssystemspezifischen Wrapper-Auswahl wurden alle fünf Skripte mit PowerShell 7.6.9 erneut auf Syntax geprüft. Der echte Aufruf von `assembleDebug.ps1` unter macOS hat alle 17 App-Projekte erfolgreich gebaut.

**Keine Windows-Ausführung durchgeführt.** Windows-Linkrechte, native Batch-Aufrufe und Ctrl+C bleiben dort zu validieren.

Die Skripte sind für Windows vorgesehen. Ein Test auf macOS/Linux ersetzt keine Prüfung von `gradlew.bat`, Windows-Symbolic-Links und Ctrl+C unter Windows. Vor produktiver Verwendung unter Windows insbesondere prüfen:

1. Aufruf aus einem fremden Arbeitsverzeichnis und aus einem Checkout mit Leerzeichen.
2. Build-/Testfolge und Weitergabe eines von Gradle gelieferten Fehlercodes.
3. Abbruch beim ersten Fehler, Zeitprotokoll und Verhalten bei Ctrl+C.
4. Generator mit gültigem/ungültigem Namen, bestehendem Ziel, Application-ID-Kollision und fehlenden Linkrechten.
5. Ein erzeugtes Projekt in Android Studio öffnen und über `gradlew.bat` bauen.
