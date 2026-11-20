# PowerShell-Skripte für Windows

Windows-Portierungen der fünf Skripte in `scripts/`. Zielumgebung ist **PowerShell 7** (`pwsh`), nicht Windows PowerShell 5.1. Unter Windows werden Bash, WSL, Git Bash und Unix-Werkzeuge nicht benötigt. Die Build-/Testskripte unterstützen auch macOS/Linux und verwenden dort den vorhandenen ausführbaren Unix-Wrapper `gradlew`.

## Voraussetzungen

- Vollständiger Checkout einschließlich `gradlew.bat`, `gradle/`, `build-logic/` und `Shared/`.
- Für Builds und Tests dieselbe Java-/Android-SDK-Konfiguration wie für die Shellskripte; insbesondere ein passendes JDK und ggf. die lokale `local.properties`.
- Für instrumentierte Tests ein gestartetes AVD oder ein autorisiertes Android-Gerät.
- Nur für die Projektgenerierung: Windows-Entwicklermodus oder Berechtigung zum Erstellen symbolischer Links. Ohne diese Rechte bricht das Skript mit einer Fehlermeldung ab und entfernt den temporären Projektordner.
- Die lokale Ausführungsrichtlinie muss die geprüften Skripte zulassen. Es wird keine systemweite Execution Policy geändert.

## Aufrufe

Aus dem Projektstamm:

~~~powershell
pwsh -NoProfile -File .\scripts_powershell\assembleDebug.ps1
pwsh -NoProfile -File .\scripts_powershell\testDebugUnitTest.ps1
pwsh -NoProfile -File .\scripts_powershell\connectedDebugAndroidTest.ps1
pwsh -NoProfile -File .\scripts_powershell\runDebugChecks.ps1
pwsh -NoProfile -File .\scripts_powershell\create-app-project.ps1 A6_01_Sensors
~~~

Unter macOS/Linux sind die fünf Skripte dank Shebang und Ausführungsrecht auch direkt startbar, sofern `pwsh` im `PATH` liegt:

~~~sh
./scripts_powershell/assembleDebug.ps1
./scripts_powershell/create-app-project.ps1 A6_01_Sensors
~~~

Unter Windows bleiben die obigen PowerShell-Aufrufe gültig.

Auch absolute Pfade mit Leerzeichen sind möglich, unabhängig vom aktuellen Verzeichnis:

~~~powershell
pwsh -NoProfile -File "C:\Android Projekte\MobileSystems\scripts_powershell\runDebugChecks.ps1"
# Innerhalb einer PowerShell-Sitzung:
& "C:\Android Projekte\MobileSystems\scripts_powershell\create-app-project.ps1" A6_01_Sensors
$LASTEXITCODE
~~~

Skripte ausführen, nicht dot-sourcen. `create-app-project.ps1` erwartet wie das Original genau **ein positionales Argument** mit dem Muster `A[1-9][0-9]*_[0-9][0-9]_[A-Za-z][A-Za-z0-9]*`.

## Verhalten und Windows-Anpassungen

- Die Skripte ermitteln ihren Projektstamm über `$PSScriptRoot`. Unter Windows rufen sie `gradlew.bat` auf, unter macOS/Linux `gradlew`; Windows-Batchdateien werden dort nicht ausgeführt. Projekte werden anhand des ursprünglichen Namensmusters in sortierter Reihenfolge verarbeitet.
- `assembleDebug.ps1` entfernt zuerst nur die lokalen Konfigurationscaches aller Apps und baut anschließend alle Apps.
- `testDebugUnitTest.ps1` prüft Shared genau einmal über das erste App-Projekt und danach die Apps mit `:testDebugUnitTest`.
- `connectedDebugAndroidTest.ps1` prüft Shared ebenfalls einmal, überspringt Module ohne Kotlin-/Java-Android-Tests, entfernt alte Ergebnisse und verlangt nach jedem erfolgreichen Gradle-Lauf mindestens eine direkt im Ergebnisordner liegende `TEST-*.xml`.
- Native Exit-Codes werden ausdrücklich geprüft und weitergereicht. Eigene Fehler enden mit Code 1; ungültige Generatorargumente mit Code 2.
- `runDebugChecks.ps1` startet Build, Unit-Tests und Android-Tests in getrennten PowerShell-Prozessen. Beim ersten Fehler endet die Folge. Ein `finally`-Block schreibt das Zeitprotokoll einschließlich nicht ausgeführter Schritte nach `logs/debug-checks-<Zeitstempel>-<PID>.log`. Wie beim Original enthält die Datei Startmeldungen und Zusammenfassung, nicht die gesamte Gradle-Ausgabe. Die Zeitzone wird als UTC-Offset ausgegeben. Bei regulärem Ctrl+C wird die Zusammenfassung bestmöglich geschrieben; gewaltsames Beenden des Prozesses kann keine Variante abfangen.
- Der Generator prüft Voraussetzungen, Zielordner und Application-ID-Kollisionen, kopiert die beiden Gradle-Vorlagen unverändert und erstellt Manifest und Activity mit denselben Inhalten und ersetztem Projektnamen. Neue Textdateien werden als UTF-8 ohne BOM mit LF geschrieben; die vorhandenen Vorlagendateien werden bytegetreu kopiert.
- Die gemeinsamen Wrapper-/Konfigurationsdateien und `gradle/` bleiben symbolisch verknüpft; eine vorhandene `local.properties` wird ebenfalls verknüpft. Für Kompatibilität auch mit PowerShell 7.0 werden absolute Linkziele verwendet. Nach einem Umzug des Checkouts müssen diese neu erzeugten Links angepasst werden. Es gibt keinen stillen Fallback auf auseinanderlaufende Dateikopien.
- Der Generator arbeitet zunächst in einem temporären Geschwisterordner. Fehler und regulärer Abbruch räumen diesen auf. Ein bereits vorhandenes Ziel wird nicht überschrieben.

## Validierung

Durchgeführt auf macOS mit PowerShell 7.4.6 über .NET: Syntaxprüfung aller fünf Skripte mit dem PowerShell-Parser und 54 kontrollierte Funktionstests in temporären Projekten mit Leerzeichen im Pfad. Gradle wurde dabei durch ein Testdouble ersetzt; für den über .NET gestarteten Testhost wurde nur die Host-Ermittlung des Koordinators gemockt. Geprüft wurden unter anderem Reihenfolge, Shared genau einmal, Fehlercodes, fehlende/veraltete/verschachtelte Berichte, Zeitprotokolle, Generatorargumente, ID-Kollisionen und Aufräumen bei fehlenden Linkrechten. Manifest, Activity und beide Gradle-Dateien wurden zusätzlich bytegenau mit den Ergebnissen des Bash-Originals verglichen. UTF-8 ohne BOM, LF, optionale lokale SDK-Konfiguration und gemeinsam genutzte Links wurden geprüft.

Nach Ergänzung der betriebssystemspezifischen Wrapper-Auswahl wurden alle fünf Skripte mit PowerShell 7.6.9 erneut auf Syntax geprüft. Der echte Aufruf von `assembleDebug.ps1` unter macOS hat alle 17 App-Projekte erfolgreich gebaut.

**Keine Windows-Ausführung durchgeführt.** Windows-Linkrechte, native Batch-Aufrufe und Ctrl+C bleiben dort zu validieren.

Die Skripte sind für Windows vorgesehen. Ein Test auf macOS/Linux ersetzt keine Prüfung von `gradlew.bat`, Windows-Symbolic-Links und Ctrl+C unter Windows. Vor produktiver Verwendung unter Windows insbesondere prüfen:

1. Aufruf aus einem fremden Arbeitsverzeichnis und aus einem Checkout mit Leerzeichen.
2. Build-/Testfolge und Weitergabe eines von Gradle gelieferten Fehlercodes.
3. Abbruch beim ersten Fehler, Zeitprotokoll und Verhalten bei Ctrl+C.
4. Generator mit gültigem/ungültigem Namen, bestehendem Ziel, Application-ID-Kollision und fehlenden Linkrechten.
5. Ein erzeugtes Projekt in Android Studio öffnen und über `gradlew.bat` bauen.
