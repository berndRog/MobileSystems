# Shared

[English](readme.md) | Deutsch

## Zweck und Verwendung

`Shared` ist die wiederverwendbare Android-Library, die jedes eigenständige App-Projekt als `:Shared` einbindet. Sie liefert Infrastruktur und UI-Bausteine; Fachregeln, Repositories, Screens und API-Schnittstellen bleiben in den Apps. Apps verwenden `implementation(project(":Shared"))` und wählen die benötigten Koin-Module aus. Öffne einen App-Ordner in Android Studio, um mit der Library zu arbeiten; `Shared` hat keine eigene startbare App.

## Wichtige Bausteine

| Bereich | Wiederverwendbarer Code |
| --- | --- |
| UI-Zustand und Effects | `EffectDelegate`, `IEffectSource`, `EffectHandler` und `SnackbarController` übertragen einmalige UI-Ereignisse. |
| Compose-Komponenten | `SwipeCard` erkennt Gesten, während der Aufrufer den Karteninhalt liefert. `InputValueString` dient der wiederverwendbaren Texteingabe; `ScrollToItemIfNotVisible` macht einen wiederhergestellten Listeneintrag sichtbar. |
| Vorläufiges Entfernen | `IVisualRemoval` und `VisualRemovalDelegate` verwalten ausgeblendete und ausstehende Einträge für Undo, ohne auf ein Repository zuzugreifen. |
| Bilder | Picker-Handler, `IImageEdit`/`ImageEdit`, Dateispeicher, MediaStore und Bilddarstellung unterstützen lokale Auswahl und Aufräumen. |
| Persistenz | `AppDatabasePerson`, `IPersonDao`, `PersonDto`, Converter und das generische Room-Datenbankmodul unterstützen Apps mit lokalen Room-Daten. |
| Netzwerk | `networkModule` stellt Retrofit, OkHttp, JSON-Konvertierung, Verbindungsprüfung, Timeouts, Logging und `NetworkExceptionMapper` bereit. Fachliche Webservice-Schnittstellen bleiben in den Apps. |
| Hilfsfunktionen | String-Ressourcen/-Provider, Logging, Eingabebereinigung, UUID-Konvertierung und Bildmodell-Hilfen. |

Die Koin-Module werden gezielt eingebunden: `utilitiesModule()`, `databaseModulePerson(...)` oder `databaseModule(...)`, `imageStorageModule(...)` und `networkModule(baseUrl, isDebug)`. Eine App registriert nur die Module für ihre Funktionen. A3_04 nutzt zum Beispiel gemeinsame Swipe- und Effect-Komponenten, A4_01 ergänzt Bilddienste und die Retrofit-Beispiele nutzen den gemeinsamen Netzwerk-Stack.

## Tests

Lokale JVM-Tests unter `src/test/java` prüfen Hilfsfunktionen, DTOs und Converter, Bildspeicherung/-bearbeitung, DI-Module, Effects, Snackbars und visuelles Entfernen. Instrumentierte Tests unter `src/androidTest/java` prüfen Android-Speicherung, Room-/DAO-Integration, Picker-Handler und Compose-Komponenten. Dafür ist ein Emulator oder Gerät nötig.

Die Repository-Skripte durchlaufen **App-Projekte**. Starte die Tests der Library deshalb ausdrücklich über den Gradle-Build einer App, zum Beispiel vom Repository-Root:

```bash
./gradlew -p A3_04_SwipeGestures :Shared:testDebugUnitTest
./gradlew -p A3_04_SwipeGestures :Shared:connectedDebugAndroidTest
```

`Shared` wird über `settings.gradle.kts` der ausgewählten App eingebunden; der zweite Befehl benötigt ein verbundenes Gerät. Nach Änderungen an gemeinsamem Verhalten sollten sowohl die Library als auch eine verwendende App geprüft werden.
