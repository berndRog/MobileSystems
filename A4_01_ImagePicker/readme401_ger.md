# A4_01_ImagePicker

## Zielsetzung und Voraussetzungen

**A4_01_ImagePicker baut auf A3_04_SwipeGestures auf** und ergänzt die Auswahl eines Fotos aus der Galerie, die Aufnahme mit der Kamera sowie Anzeige, Entfernen und Ersetzen des Bildes. Ein zweiter Schwerpunkt ist die **Einführung von Use Cases in der Domain**: Operationen, die Personenpersistenz und Bildverwaltung verbinden, werden aus dem ViewModel ausgelagert. Die Liste (`PeopleAdapter`/`PeopleScreen`) behält Swipe-to-Detail und Löschen mit Snackbar-Bestätigung bei. Das Formular (`PersonAdapter`/`PersonScreen`) erhält Bildaktionen. Die Undo-Mechanik aus A3_05 bleibt eine separate Erweiterung.

Vorausgesetzt werden Compose, State Hoisting, Intents, ViewModel-State, Effects, Navigation 3 und der bestätigte Löschablauf aus A3_04.

## Lernziele

Nach der Bearbeitung können Studierende:

- Android Photo Picker und externe Kamera über getrennte Activity-Result-Handler anbinden.
- Content-URI der Galerie, FileProvider-URI für die Kamera und internen Bildpfad unterscheiden.
- Android-Picker-Aufrufe in Adapter/Handlern halten, während der Screen Werte und Callbacks erhält.
- Die Verantwortlichkeiten von `IImageFileStorage`, `IImageEdit`, ViewModel und Personen-Use-Cases erklären.
- Use Cases in `domain/usecases` einführen, über `suspend operator fun invoke()` aufrufen und die Koordination einer Anwendungsoperation von UI-State-Verwaltung und Repository-Zugriff abgrenzen.
- Originalbilder bis zum erfolgreichen Speichern erhalten und ungespeicherte Bilder bei Cancel verwerfen.
- Bild-State bis zur Anzeige und Bildfehler vom ViewModel über Effects bis zur Snackbar verfolgen.

## Zwei konkrete Bildabläufe

| Quelle | Handler in `Shared` | Verarbeitung |
| --- | --- | --- |
| Galerie | `GalleryPickerHandlerSingle` | Photo Picker → Content-URI → `GalleryImageSelected`-Intent → Import über den ImageEditDelegate |
| Kamera | `CameraPickerHandler` | Zieldatei/URI vorbereiten → Kamera → Datei bestätigen → `CameraImageTaken`-Intent mit internem Pfad |

**Galerie:** `PickVisualMedia` wählt ein Bild aus. Beim Abbrechen entsteht kein Auswahlereignis. Das ViewModel delegiert den Import an `IImageEditDelegate.importGallery()`. Der Delegate kopiert das Bild über den Dateispeicher und liefert die geänderte Auswahl als `Result<ImageChange>`. Gespeichert wird ein app-eigener Bildpfad statt der Picker-URI.

**Kamera:** `TakePicture` benötigt vor dem Start ein beschreibbares Ziel und liefert einen Boolean zurück. Der Handler legt eine vorläufige Datei an und übergibt deren FileProvider-URI an die externe Kamera. Er merkt sich den zugehörigen internen Pfad, bestätigt eine erfolgreiche Aufnahme und versucht bei Abbruch oder Fehler aufzuräumen. Der FileProvider ist in `Shared` deklariert.

Anschließend laufen beide Wege zusammen: **Galerie-URI/Kamerapfad → ImageEditDelegate → `ImageChange` → `PersonUiState.person.imagePath` → Bildanzeige**.

## Didaktischer Ansatz

Ausgangspunkt sind die sichtbaren Aktionen; danach werden ihre unterschiedlichen technischen Voraussetzungen verglichen. `PersonScreen` nutzt `ImageSelection` und erhält `onSelectPhoto`, `onTakePhoto` und `onRemovePhoto`. `PersonAdapter` verbindet diese Callbacks mit Handlern beziehungsweise Intents und beobachtet State lifecycle-aware. `ImageRenderer` verwendet Coils `AsyncImage` oder ein Platzhalter-Icon, wenn kein Bild vorhanden ist.

Dateibeschaffung und Verwaltung der Edit-Session werden getrennt betrachtet. `IImageFileStorage` führt Dateioperationen aus; `ImageEdit` merkt sich ursprüngliche und aktuell ausgewählte Bilder. `ImageEditDelegate` koordiniert Import, Kameraübernahme, Ersetzen, Entfernen und abgewiesene Ergebnisse. Das ViewModel benötigt weder Dateispeicher noch ImageEdit direkt. Es verwendet `ImageSelectionMode.REPLACE` und `maxImages = 1`.

| Aktion | Verhalten der Bild-Session |
| --- | --- |
| Bild ersetzen/entfernen | Auswahl ändern; überholte ungespeicherte Ersatzbilder verwerfen, persistierte Originale erhalten |
| Save erfolgreich | Create-/Update-Use-Case speichert die Person und ruft danach `ImageEdit.commit()` auf; ungenutzte Originale dürfen gelöscht werden |
| Save fehlgeschlagen | Original und Ersatzbild für Retry oder Cancel erhalten |
| Cancel/System Back | Mit `discard()` ungespeicherte Bilder entfernen, danach `NavigateBack` ausgeben |

## Einführung von Use Cases in der Domain

A4_01 führt `PersonUcCreate`, `PersonUcUpdate` und `PersonUcDelete` in `domain/usecases` ein. Durch die Bildverwaltung umfasst jede Anwendungsoperation mehrere Schritte und Abhängigkeiten. Ein Use Case bündelt diese Schritte zu einer Operation mit festgelegter Reihenfolge:

| Use Case | Koordinierte Operation |
| --- | --- |
| `PersonUcCreate` | `repository.create(person)` → bei Erfolg `imageEdit.commit()` |
| `PersonUcUpdate` | `repository.update(person)` → bei Erfolg `imageEdit.commit()` |
| `PersonUcDelete` | Personen-ID → `repository.findById(id)` → aktuelle Person löschen → bei Erfolg Bilddatei aufräumen |

Das ViewModel verarbeitet Intents, verwaltet UI-State, validiert das Formular, wählt den passenden Use Case und übersetzt dessen Ergebnis in Effects. Der Use Case koordiniert die Anwendungsoperation; das Repository stellt den Zugriff auf Personendaten bereit. Abhängigkeiten werden über Constructor Injection übergeben. Mit `suspend operator fun invoke(person)` kann das ViewModel eine Use-Case-Instanz wie eine Funktion aufrufen, etwa `_personUcUpdate(person)`.

Der Lernschritt besteht darin, die zusammengesetzte Persistenz-/Bildoperation aus dem ViewModel auszulagern und ihre Reihenfolge unabhängig zu testen. Ein fehlgeschlagener Repository-Schreibzugriff führt nicht zum Commit der Bild-Session; bei fehlgeschlagener Personenlöschung bleibt das Bild erhalten. Aufräumfehler nach erfolgreicher Personenlöschung werden protokolliert.

Fehler beim Kopieren und Kamera-Fehler werden zu `ShowError`-Effects aus dem ViewModel und in einer Snackbar ausgegeben. Erfolgreiches Speichern erzeugt `ShowMessage`, danach `NavigateBack`. Der Bildpfad bleibt State; die Anforderung einer Meldung oder Navigation ist ein Effect.

## Generalisierte Effect-Verarbeitung

Wie in A3_02 und A3_04 verwenden beide ViewModels den `EffectDispatcher` mit ihrem bestehenden `EffectDelegate`. `launchShowError` und `launchEmit` starten den Versand aus Intents; `showError`, `showMessage` und `emit` bleiben innerhalb laufender Operationen in deren Coroutine. Die Speicherbestätigung wird vor der Rücknavigation gesendet. Bildoperationen und Save behalten ihren `UNDISPATCHED`-Start, alle Guards und Completion-Handler bei.

Einfache Meldungsressourcen und Fehlerlogging übernimmt der Dispatcher. Bereits protokollierte Fehler beim Abweisen eines Kamerabildes werden ohne Throwable weitergereicht, damit kein zweiter Logeintrag entsteht. Ladefehler bleiben im UiState; Text und Action-Label der Löschbestätigung bleiben im ViewModel. Bildimport, Commit, Discard und Cleanup behalten ihre bisherigen Verantwortlichkeiten und Lebensdauern.

## Abgrenzung zu Kapitel 5

ViewModels und die drei Personen-Use-Cases verarbeiten erwartete Fehler ausschließlich mit `.onSuccess` und `.onFailure`. Sie verwenden weder `try/catch/finally` noch `getOrThrow()` oder `resultOf()`. Cancellation wird weitergegeben; `ensureActive()` verhindert verspätete Seiteneffekte, `cancelAndJoin()` wartet bei Retry auf den vorherigen Collector. Completion-Handler setzen ausschließlich `isSaving` und `isEditingImages` zurück und führen kein Cleanup aus.

Persistierung erfolgt vor dem Image-Commit. Result-Fehler beim nachgelagerten Datei-Cleanup werden protokolliert, ohne eine erfolgreiche Löschung als Fehlschlag auszugeben. Koin erzeugt pro Detail-ViewModel eine gemeinsame ImageEdit-Session für Delegate und beide Save-Use-Cases. `PersonDependencyInjectionTest` sichert dies ab.

Bewusste Grenze: Unerwartet geworfene gewöhnliche Exceptions aus Repository-, Commit- oder Cleanup-Implementierungen werden in diesen Use Cases nicht zusätzlich abgefangen oder in Results umgewandelt. Ein unerwarteter Cleanup-Wurf wird nicht als Best-Effort-Result behandelt. Im ViewModel sind dann Fehlermeldung, Ladezustandsrücksetzung und erneutes Cancel nicht allgemein garantiert; die Save-/Image-Flags werden weiterhin bei Job-Abschluss zurückgesetzt. Die bestehenden technischen Absicherungen in Repository, Datenbankinitialisierung und Shared bleiben erhalten. Die produktiven Shared-Komponenten liefern erwartete Dateifehler über Results beziehungsweise behandeln Session-Cleanup intern.

Die vorhandene Room-Infrastruktur in Shared bleibt bereitgestellt und wird hier nicht neu erklärt. Kapitel 5 macht die Persistenz sichtbar und führt DTO, ReadModel, Listenprojektion, Formmodell sowie die erweiterte Exception-Behandlung ein. A4 verwendet weiterhin `Person` als Eingabe- und Domainmodell; `PersonInputMapping.kt` enthält die Eingabenormalisierung. `IPersonRepository` liegt bereits in `domain.repositories`.

Save-Overlay, Fortschrittsanzeige für Bildoperationen, Picker-Handler, Back-Verhalten und Snackbar-Abstände zu Tastatur und FAB bilden die gemeinsame UI-Basis mit A5. Die Löschbestätigung erhält genau einen vollständigen Namen; Tests verwenden dafür den echten StringProvider mit deutschen und englischen Ressourcen.

## Vorgeschlagener Lernpfad

1. **Auswählen und anzeigen:** Formular öffnen, Galeriebild auswählen und URI → interner Pfad → State → Vorschau verfolgen.
2. **Foto aufnehmen:** Zielerstellung, `TakePicture`-Ergebnis und Dateibestätigung mit dem Galerieablauf vergleichen. Eine Aufnahme auch abbrechen.
3. **Save und Cancel vergleichen:** Vorhandenes Bild ersetzen und abbrechen; anschließend wiederholen und speichern. Erklären, wann alte und neue Dateien entfernt werden dürfen.
4. **Use Cases einführen:** Den Repository-Aufruf im ViewModel aus A3_04 mit dem Aufruf eines Domain-Use-Case in A4_01 vergleichen. UI-State, Operationskoordination und Datenzugriff ViewModel, Use Case und Repository zuordnen.
5. **Fehlerpfade untersuchen:** In `PersonViewModelImageTest` und `PersonUseCasesTest` fehlgeschlagenes Kopieren, Aufräumen von Ersatzbildern, fehlgeschlagenes Speichern und Personenlöschung mit Bild nachvollziehen.
6. **An A3_04 anknüpfen:** Bestätigtes Swipe-to-Delete bis `PersonUcDelete` verfolgen und den zusätzlichen Schritt der Dateibereinigung erklären.

Als Lernnachweis eignen sich beide Beschaffungsabläufe als Skizze mit Zuordnung von Original- und Ersatzbildern während Save und Cancel.

Quellcodebasis: [MobileSystems, Revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A4_01_ImagePicker), einschließlich Bildkomponenten, Dateispeicher und FileProvider-Konfiguration in `Shared`.
