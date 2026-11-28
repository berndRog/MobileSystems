# A3_02_EffectHandling

## Zielsetzung und Voraussetzungen

Dieses Modul erweitert **A3_01_Material** um **Effects, die vom ViewModel ausgehen**. Ein Effect teilt der UI mit, welche Aktion sie aufgrund eines Ergebnisses oder einer Entscheidung im ViewModel ausführen soll. In A3_02 werden damit Meldungen oder Fehler in einer Snackbar ausgegeben. Ab A3_03 werden Effects zusätzlich genutzt, um die Navigation zwischen Screens anzufordern.

Beispiel: Nach erfolgreichem Speichern einer Person gibt `PersonViewModel` einen `ShowMessage`-Effect aus. Schlägt das Speichern fehl, gibt es `ShowError` aus. Der Adapter empfängt den Effect und leitet dessen Text an die Snackbar-Ausgabe weiter. Der Anstoß kommt vom ViewModel; die UI führt die Aktion aus.

Vorausgesetzt werden Kenntnisse in Compose, State Hoisting, der Trennung von Adapter und Screen, Intents und der grundlegenden Verwendung von `StateFlow` aus A3_01.

## Lernziele

Nach der Bearbeitung können Studierende:

- `UiState`, der den aktuellen Screen-Zustand beschreibt, von Effects unterscheiden, mit denen das ViewModel eine UI-Aktion anfordert: `ShowMessage` zeigt eine Meldung in einer Snackbar, `ShowError` einen Fehler. Erklären, wie Navigationseffekte diesen Mechanismus in A3_03 erweitern.
- Feature-spezifische Effects mit sealed Interfaces modellieren (`PersonEffect`, `PeopleEffect`).
- Erklären, wie `EffectDelegate<E>` einen gepufferten `Channel` über `receiveAsFlow()` bereitstellt und Kotlin-Interfacedelegation mit `by` wiederholte Infrastruktur in den ViewModels vermeidet.
- EffectDispatcher für Ressourcen, Formatargumente, typisierte Meldungen und zentrales Fehlerlogging einsetzen.
- Suspendierende Aufrufe von launchShowError, launchShowMessage und launchEmit unterscheiden.
- Generisches Sammeln von Effects und deren Feature-spezifische Verarbeitung im Adapter trennen.
- Meldungs-Callbacks über `SnackbarController` und `SnackbarHostState` mit der Snackbar-Ausgabe von Material 3 verbinden.
- Ladefehler im State sichtbar halten, einen expliziten Retry anbieten und State-Änderungen getrennt von ausgegebenen Effects testen.

## Didaktischer Ansatz

Die Architektur aus A3_01 bleibt erhalten. Hinzu kommt ein zweiter Ausgabepfad des ViewModel:

**Benutzeraktion → Intent → ViewModel → EffectDispatcher → EffectDelegate/IEffectSource → EffectHandler im Adapter → SnackbarController**

Intents transportieren Benutzeraktionen zum ViewModel. Effects transportieren daraus entstehende Anforderungen vom ViewModel zur UI. Beim erfolgreichen Speichern lautet der konkrete Ablauf: `PersonIntent.Save` → Speichern im Repository → `PersonEffect.ShowMessage` → `onMessage` → `SnackbarController.showMessage()`. Bei einem Speicherfehler werden stattdessen `ShowError`, `onError` und `showError()` verwendet. In A3_03 erweitern `NavigateTo` und `NavigateBack` diesen Ausgabepfad um Navigation; die Navigationsschicht verändert daraufhin den Backstack.

| Information | Beispiel | Verarbeitung |
| --- | --- | --- |
| Dauerhafter State | Personendaten, `isLoading`, `isSaving`, `loadFailure` | `StateFlow` → Adapter → Screen |
| Einmaliger Effect | Speicherbestätigung oder Aktionsfehler | Effect-Flow → Adapter → Meldungs-Callback → Snackbar |

Ein Ladefehler bleibt relevant, bis ein weiterer Ladeversuch erfolgreich ist. Er gehört deshalb in den State und wird zusammen mit Retry angezeigt. Eine Speicherbestätigung oder ein Fehler beim Speichern beziehungsweise Löschen betrifft eine einzelne Aktion und wird als Effect ausgegeben. Die Validierungsrückmeldung am Formular bleibt bestehen; die abschließende Prüfung vor dem Speichern kann zusätzlich `ShowError` auslösen.

`EffectHandler` sammelt den Effect-Flow in `LaunchedEffect`. Der Adapter wertet den konkreten Effect aus und ruft `onMessage` oder `onError` auf. `SnackbarController` kapselt die Coroutine für den suspendierenden Aufruf von `showSnackbar()`. Meldungen erscheinen kurz; Fehler länger und mit einer Möglichkeit zum Schließen. Der EffectDispatcher löst Meldungsressourcen über `IStringProvider` auf; Effects transportieren fertige Strings. Persistente Ladefehler werden weiterhin im ViewModel lokalisiert und im UiState gespeichert.

Pro Feature-Effect-Flow ist ein UI-Consumer vorgesehen: Der zugrunde liegende Channel verteilt Ereignisse zwischen mehreren Consumern, statt jedes Ereignis an alle zu senden.

## Referenzimplementierung mit EffectDispatcher

A3_02 führt die generalisierte Effect-Verarbeitung bereits zusammen mit EffectDelegate, IEffectSource, EffectHandler und SnackbarController ein. Die direkte Delegate-Verwendung wird in den Vorlesungsunterlagen als Grundlage erklärt. Die nachfolgenden Projekte übernehmen dieses Muster nach gesonderter Freigabe.

Jedes ViewModel erzeugt einen lokalen `EffectDispatcher<PersonEffect>` beziehungsweise `EffectDispatcher<PeopleEffect>`. Factory-Funktionen erzeugen die Feature-Effects. Dispatcher und die per `by` delegierte IEffectSource verwenden dieselbe injizierte EffectDelegate-Instanz. Koin, Effect-Definitionen, Adapter und SnackbarController bleiben unverändert.

Innerhalb der laufenden Coroutine genügen `_effects.showError(R.string.error_person_save, throwable)` und `_effects.showMessage(R.string.message_person_saved, person.fullName)`. Außerhalb einer Coroutine startet `_effects.launchShowError(error)` den Versand eines Validierungstextes. Nur die launch-Methoden starten neue Coroutines; die suspendierenden Methoden bleiben im Kontext des Aufrufers.

Der Dispatcher protokolliert einen übergebenen technischen Fehler einmal. Der Aufrufer loggt denselben Meldungsfehler nicht erneut. Validierungstexte ohne Throwable erzeugen keinen zusätzlichen Fehlerlog. Cancellation wird weder als Snackbar noch als persistenter Ladefehler ausgegeben. Ein Completion-Handler setzt beim Speichern ausschließlich isSaving zurück, auch nach Cancellation; er sendet keine Effects. Unerwartete Exceptions werden hier weiterhin nicht umfassend behandelt.

Die Shared-Tests prüfen Ressourcenargumente, Factories, Logging, optionale Message-Factory, Cancellation, Scope-Verhalten, Buffering und Effect-Reihenfolge. Die ViewModel-Tests sichern Fachverhalten, getrennte Kanäle, einmalige Fehlermeldungen und dauerhafte Ladefehler ab. PersonSnackbarTest prüft den vollständigen UI-Pfad.

## Vorgeschlagener Lernpfad

1. **Mit A3_01 vergleichen:** Die Löschaktion der Liste verfolgen und erkennen, wo Logging um `ShowMessage` beziehungsweise `ShowError` ergänzt wird.
2. **Beide Pfade verfolgen:** Eine State-Änderung und einen Meldungs-Effect vom ViewModel bis zur jeweiligen Verarbeitung in der UI nachvollziehen.
3. **Speichern untersuchen:** In `ui/MainActivity.kt` den `PersonAdapter` statt des `PeopleAdapter` auswählen und die Snackbar-Parameter sowie Callbacks beibehalten. Als Personen-ID `null` für den Anlegemodus oder eine vorhandene ID für den Detailmodus verwenden. Ungültige Eingaben und erfolgreiches Speichern vergleichen.
4. **Fehlerfälle untersuchen:** `PersonViewModelEffectTest` und `PeopleViewModelEffectTest` lesen. Die getesteten Unterschiede zwischen dauerhaftem Ladefehler, Retry, Speicherfehler-Effect und gesperrten Aktionen während des Speicherns erklären.

Als Lernnachweis eignen sich eine kommentierte Darstellung beider Ausgabepfade und die begründete Einordnung eines Ladefehlers sowie eines Aktionsergebnisses.

## Einordnung in die Lernprogression

A3_01 vermittelt Material-Oberfläche, Validierung, Mapping und State. **A3_02 ergänzt EffectHandling unabhängig von Navigation.** `MainActivity` zeigt aktuell die Liste; Anlege-/Detail-Screen werden manuell ausgewählt. Create- und Detail-Intents navigieren noch nicht; der Toolbar-Pfeil des Formulars löst Save aus. Cancel leert Vor- und Nachnamen.

A3_03 verbindet die Screens über Navigation 3 und Navigationseffekte. A3_04 ergänzt Swipe-Gesten mit Snackbar-Bestätigung vor dem Löschen; Undo folgt in A3_05.

Quellcodebasis: [MobileSystems, Revision 6e82a97](https://github.com/berndRog/MobileSystems/tree/6e82a973e40dfe863076ac227a70fca73396e8b4/A3_02_EffectHandling), einschließlich der Effect-Infrastruktur in `Shared`.
