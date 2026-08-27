package de.rogallab.mobile.ui.people.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.effects.IEffectSource
import de.rogallab.mobile.shared.ui.removal.IVisualRemoval
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PeopleViewModel(
   private val _repository: IPersonRepository,
   private val _stringProvider: IStringProvider,
   private val _visualRemoval: IVisualRemoval<Person>,
   private val _effectDelegate: EffectDelegate<PeopleEffect>,
) : ViewModel(), IEffectSource<PeopleEffect> by _effectDelegate {

   // Holds the persistent UI state of the people screen.
   private val _stateFlow: MutableStateFlow<PeopleUiState> =
      MutableStateFlow(PeopleUiState())

   // Exposes the state as a read-only StateFlow to the UI.
   val stateFlow: StateFlow<PeopleUiState> =
      _stateFlow.asStateFlow()

   // Job used to observe changes from the repository.
   private var _observeJob: Job? = null

   init {
      Alog.i(TAG, "init: observePeople()")
      observePeople()
   }

   // Dispatches incoming UI intents to the corresponding action.
   fun onIntent(intent: PeopleIntent) {
      Alog.d(TAG, "intent: $intent")

      when (intent) {
         PeopleIntent.Create -> navigateToPerson(null)
         is PeopleIntent.Detail -> navigateToPerson(intent.personId)
         is PeopleIntent.Remove -> removeVisually(intent.person)
         is PeopleIntent.UndoRemove -> undoRemove(intent.personId)
         is PeopleIntent.CommitRemove -> commitRemove(intent.personId)
      }
   }

   // Emits the navigation effect. A null id opens the create destination.
   private fun navigateToPerson(personId: String?) {
      viewModelScope.launch {
         _effectDelegate.emit(
            PeopleEffect.NavigateTo(personId)
         )
      }
   }

   // Observes the repository. The delegate combines this persistent source
   // list with the temporary items that are hidden during an Undo operation.
   private fun observePeople() {
      _observeJob?.cancel()

      _observeJob = viewModelScope.launch {

         // Show the loading indicator until the first result arrives.
         _stateFlow.update { state: PeopleUiState ->
            state.copy(isLoading = true)
         }

         // Simulate a longer loading operation.
         delay(1000)

         _repository.observeAll().collect { result: Result<List<Person>> ->
            result
               .onSuccess { people ->
                  // Pass the latest persistent source list to the delegate.
                  _visualRemoval.update(people)
                  publishVisiblePeople(isLoading = false)
                  Alog.d(TAG, "observePeople: people=${people.size}")
               }
               .onFailure { throwable ->
                  _stateFlow.update { state: PeopleUiState ->
                     state.copy(isLoading = false)
                  }

                  Alog.e(TAG, "observePeople failed: ${throwable.message}")
                  _effectDelegate.emit(
                     PeopleEffect.ShowError(
                        _stringProvider.getString(R.string.error_people_observe)
                     )
                  )
               }
         }
      }
   }

   // Removes a person only from the visible state. The delegate keeps the
   // temporary removal state; the repository is not touched during Undo.
   private fun removeVisually(person: Person) {
      // Repeated events for the same item must not create another Undo effect.
      if (!_visualRemoval.remove(person)) return

      publishVisiblePeople()

      viewModelScope.launch {
         _effectDelegate.emit(
            PeopleEffect.ShowUndo(
               message = _stringProvider.getString(
                  R.string.message_person_removed,
                  person.fullName,
               ),
               actionLabel = _stringProvider.getString(R.string.action_undo),
               personId = person.id,
            )
         )
      }
   }

   // Cancels a pending deletion. The delegate removes the temporary filter so
   // that the unchanged repository item becomes visible again immediately.
   private fun undoRemove(personId: String) {
      if (!_visualRemoval.undo(personId)) return

      publishVisiblePeople()
      Alog.d(TAG, "undoRemove: personId=$personId")
   }

   // Deletes from the repository only after the Action Snackbar was dismissed
   // without selecting Undo.
   private fun commitRemove(personId: String) {
      val person = _visualRemoval.pending(personId) ?: return

      viewModelScope.launch {
         _repository.remove(person)
            .onSuccess {
               // End the pending Undo operation. The delegate keeps the item
               // hidden until observeAll() confirms the repository change.
               _visualRemoval.commit(personId)
               Alog.d(TAG, "commitRemove: personId=$personId")
            }
            .onFailure { throwable ->
               // Persistence failed: restore the visual item and report error.
               _visualRemoval.restore(personId)
               publishVisiblePeople()

               Alog.e(TAG, "commitRemove failed: ${throwable.message}")
               _effectDelegate.emit(
                  PeopleEffect.ShowError(
                     _stringProvider.getString(R.string.error_person_remove)
                  )
               )
            }
      }
   }

   // Publishes the source list after the delegate has applied its temporary
   // UI-only removal filter.
   private fun publishVisiblePeople(
      isLoading: Boolean = _stateFlow.value.isLoading,
   ) {
      _stateFlow.update { state: PeopleUiState ->
         state.copy(
            people = _visualRemoval.visibleItems(),
            isLoading = isLoading,
         )
      }
   }

   companion object {
      private const val TAG = "<-PeopleViewModel"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - PeopleUiState bleibt wie in den vorherigen Beispielen der einzige State,
 *   den die Oberfläche beobachtet. MutableStateFlow wird nur innerhalb des
 *   ViewModels verändert und nach außen als read-only StateFlow angeboten.
 *
 * - Neu ist VisualRemovalDelegate. Die drei Verwaltungsstrukturen für den
 *   Repository-Stand, visuell ausgeblendete ids und ausstehende Löschungen
 *   liegen nicht mehr direkt im ViewModel. Diese technische Zustandsverwaltung
 *   wird über IVisualRemoval<Person> an ein spezialisiertes Objekt delegiert.
 *
 * - Dabei handelt es sich bewusst nicht um Kotlin Interface Delegation mit
 *   "by". PeopleViewModel implementiert IVisualRemoval<Person> nicht, sondern
 *   besitzt die Abhängigkeit und ruft ihre Funktionen explizit auf. Dies ist
 *   Delegation einer Aufgabe durch Komposition.
 *
 * - Effects zeigen im selben ViewModel die andere Form: Durch
 *   IEffectSource<PeopleEffect> by _effectDelegate implementiert PeopleViewModel
 *   formal die Effect-Schnittstelle, während EffectDelegate deren Implementierung
 *   bereitstellt. Damit können beide Delegationsformen direkt verglichen werden.
 *
 * - Remove verändert weiterhin zunächst ausschließlich den sichtbaren Zustand.
 *   Das Repository bleibt während des Undo-Fensters unverändert. ShowUndo wird
 *   als einmaliger Effect erzeugt und gehört deshalb nicht in PeopleUiState.
 *
 * - UndoRemove hebt nur den temporären Filter im VisualRemovalDelegate auf.
 *   Eine Repository-Operation muss nicht rückgängig gemacht werden, weil die
 *   persistente Löschung zu diesem Zeitpunkt noch gar nicht erfolgt ist.
 *
 * - Erst CommitRemove führt _repository.remove(...) aus. Nach Erfolg beendet
 *   commit(...) den Pending-Zustand; nach Fehler stellt restore(...) das Element
 *   wieder her und das ViewModel erzeugt zusätzlich ShowError.
 *
 * - Der Delegate kennt weder Repository, StateFlow, Coroutines, Strings noch
 *   Effects. Diese Verantwortungen bleiben im ViewModel. Dadurch wird das
 *   ViewModel kürzer, ohne die eigentliche Anwendungslogik zu verstecken.
 *
 * Lernziele:
 *
 * - State, Intent und Effect weiterhin konsequent voneinander unterscheiden.
 * - Temporären UI-Zustand von persistiertem Repository-State trennen.
 * - Kotlin Interface Delegation und Delegation durch Komposition unterscheiden.
 * - Dependency Injection zur Bereitstellung eines Delegationsobjekts nutzen.
 * - Undo vor einer destruktiven Persistenzoperation implementieren.
 * - ViewModels durch klar abgegrenzte, testbare Verantwortlichkeiten lesbar halten.
 */
