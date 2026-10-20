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
| Galerie | `GalleryPickerHandlerSingle` | Photo Picker → Content-URI → `GalleryImageSelected`-Intent → Kopieren in den App-Speicher im ViewModel |
| Kamera | `CameraPickerHandler` | Zieldatei/URI vorbereiten → Kamera → Datei bestätigen → `CameraImageTaken`-Intent mit internem Pfad |

**Galerie:** `PickVisualMedia` wählt ein Bild aus. Beim Abbrechen entsteht kein Auswahlereignis. Das ViewModel kopiert das gewählte Bild über `IImageFileStorage.copyImageToAppStorage()`. Dieses Modul speichert einen app-eigenen Bildpfad statt der Picker-URI.

**Kamera:** `TakePicture` benötigt vor dem Start ein beschreibbares Ziel und liefert einen Boolean zurück. Der Handler legt eine vorläufige Datei an und übergibt deren FileProvider-URI an die externe Kamera. Er merkt sich den zugehörigen internen Pfad, bestätigt eine erfolgreiche Aufnahme und versucht bei Abbruch oder Fehler aufzuräumen. Der FileProvider ist in `Shared` deklariert.

Anschließend laufen beide Wege zusammen: **interner Pfad → `IImageEdit.replace()` → `PersonUiState.person.imagePath` → Bildanzeige**.

## Didaktischer Ansatz

Ausgangspunkt sind die sichtbaren Aktionen; danach werden ihre unterschiedlichen technischen Voraussetzungen verglichen. `PersonScreen` nutzt `ImageSelection` und erhält `onSelectPhoto`, `onTakePhoto` und `onRemovePhoto`. `PersonAdapter` verbindet diese Callbacks mit Handlern beziehungsweise Intents und beobachtet State lifecycle-aware. `ImageRenderer` verwendet Coils `AsyncImage` oder ein Platzhalter-Icon, wenn kein Bild vorhanden ist.

Dateibeschaffung und Verwaltung der Edit-Session werden getrennt betrachtet. `IImageFileStorage` führt Dateioperationen aus; `ImageEdit` merkt sich ursprüngliche und aktuell ausgewählte Bilder. Eine Person verwendet ein Bild, der Delegate nimmt jedoch Listen von Pfaden entgegen.

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
| `PersonUcDelete` | `repository.remove(person)` → bei Erfolg Aufräumen der Bilddatei versuchen |

Das ViewModel verarbeitet Intents, verwaltet UI-State, validiert das Formular, wählt den passenden Use Case und übersetzt dessen Ergebnis in Effects. Der Use Case koordiniert die Anwendungsoperation; das Repository stellt den Zugriff auf Personendaten bereit. Abhängigkeiten werden über Constructor Injection übergeben. Mit `suspend operator fun invoke(person)` kann das ViewModel eine Use-Case-Instanz wie eine Funktion aufrufen, etwa `_personUcUpdate(person)`.

Der Lernschritt besteht darin, die zusammengesetzte Persistenz-/Bildoperation aus dem ViewModel auszulagern und ihre Reihenfolge unabhängig zu testen. Ein fehlgeschlagener Repository-Schreibzugriff führt nicht zum Commit der Bild-Session; bei fehlgeschlagener Personenlöschung bleibt das Bild erhalten. Aufräumfehler nach erfolgreicher Personenlöschung werden protokolliert.

Fehler beim Kopieren und Kamera-Fehler werden zu `ShowError`-Effects aus dem ViewModel und in einer Snackbar ausgegeben. Erfolgreiches Speichern erzeugt `ShowMessage`, danach `NavigateBack`. Der Bildpfad bleibt State; die Anforderung einer Meldung oder Navigation ist ein Effect.

## Vorgeschlagener Lernpfad

1. **Auswählen und anzeigen:** Formular öffnen, Galeriebild auswählen und URI → interner Pfad → State → Vorschau verfolgen.
2. **Foto aufnehmen:** Zielerstellung, `TakePicture`-Ergebnis und Dateibestätigung mit dem Galerieablauf vergleichen. Eine Aufnahme auch abbrechen.
3. **Save und Cancel vergleichen:** Vorhandenes Bild ersetzen und abbrechen; anschließend wiederholen und speichern. Erklären, wann alte und neue Dateien entfernt werden dürfen.
4. **Use Cases einführen:** Den Repository-Aufruf im ViewModel aus A3_04 mit dem Aufruf eines Domain-Use-Case in A4_01 vergleichen. UI-State, Operationskoordination und Datenzugriff ViewModel, Use Case und Repository zuordnen.
5. **Fehlerpfade untersuchen:** In `PersonViewModelImageTest` und `PersonUseCasesTest` fehlgeschlagenes Kopieren, Aufräumen von Ersatzbildern, fehlgeschlagenes Speichern und Personenlöschung mit Bild nachvollziehen.
6. **An A3_04 anknüpfen:** Bestätigtes Swipe-to-Delete bis `PersonUcDelete` verfolgen und den zusätzlichen Schritt der Dateibereinigung erklären.

Als Lernnachweis eignen sich beide Beschaffungsabläufe als Skizze mit Zuordnung von Original- und Ersatzbildern während Save und Cancel.

Quellcodebasis: [MobileSystems, Revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A4_01_ImagePicker), einschließlich Bildkomponenten, Dateispeicher und FileProvider-Konfiguration in `Shared`.
