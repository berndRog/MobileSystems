# Teststruktur A3_01 bis A3_05

Die Tests folgen den normalen Gradle/Android-Source-Sets eines Moduls:

- `src/test/java` für lokale JVM-Tests. Diese Tests sind schnell und eignen sich
  für Mapping, Validatoren, ViewModels, Effects und Repository-Fakes.
- `src/androidTest/java` für instrumentierte Tests auf Emulator/Gerät. Diese
  werden nur dort eingesetzt, wo Android-/Compose-Verhalten selbst geprüft
  werden soll, hier insbesondere die Swipe-Gesten in A3_05.

Die Testpackages spiegeln die fachlichen Production-Packages. Gemeinsame
Testhilfen eines Moduls liegen unter `de.rogallab.mobile.testing`.

## Lernprogression

- A3_01_Material: Mapping, Validation und grundlegender ViewModel-State.
- A3_02_EffectHandling: einmalige ShowMessage/ShowError-Effects sowie die
  vorbereitete Back-Navigation.
- A3_03_Navigation: NavigateTo/NavigateBack-Effects und Navigation 3. Die
  Snackbar-Infrastruktur benötigt keinen eigenen Coordinator-Test mehr, weil
  die Anwendung Material3 `SnackbarHostState` direkt verwendet.
- A3_04_ImagePicker: Bildpfad-State und Lebenszyklus temporärer/ersetzter Bilder,
  einschließlich erfolgreichem und fehlgeschlagenem Speichern.
- A3_05_SwipeGestures: visuelles Entfernen, Undo, verzögertes Repository-Commit,
  Fehler beim Commit und instrumentierte Swipe-Richtungen.
- Shared: `SnackbarController` und `EffectDelegate` als
  wiederverwendbare Basis. Neben dem Effect-Transport werden auch die direkten
  Message-/Error-Aufrufe sowie beide Action-Ergebnisse (`performAction`/`dismiss`) des
  `SnackbarController` lokal getestet.

## Robolectric und API 37

Die Beispielmodule verwenden `targetSdk = 37`. Robolectric 4.16.1 unterstützt
für lokale Tests jedoch nur niedrigere SDK-Level. Android-nahe JVM-Tests werden
mit `@Config(sdk = [35])` auf einem unterstützten Test-SDK ausgeführt. Die
Produktionskonfiguration bleibt davon unverändert.

`MainDispatcherRule` schaltet außerdem `Alog` für lokale Tests auf die
Nicht-Android-Ausgabe um. Dadurch rufen reine JVM-Tests nicht versehentlich
`android.util.Log` auf.

## Alternative Strukturierung

Tests müssen nicht physisch neben den Production-Dateien liegen. Sie müssen
aber einem Gradle-Test-Source-Set zugeordnet sein. Bei größeren Projekten kann
man gemeinsame Fakes und Testregeln über Gradle `testFixtures` oder ein eigenes
Test-Support-Modul bereitstellen. Für die Lehrmodule bleiben die wenigen
Hilfsklassen absichtlich im jeweiligen Modul, damit jedes Beispiel eigenständig
lesbar und ausführbar bleibt.
