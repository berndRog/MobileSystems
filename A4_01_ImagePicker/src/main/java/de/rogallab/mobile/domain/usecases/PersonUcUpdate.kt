package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.images.IImageEdit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException

class PersonUcUpdate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {
      currentCoroutineContext().ensureActive()
      return _repository.update(person)
         .onSuccess {
            // Commit only after persistence; cancellation must keep the session open.
            currentCoroutineContext().ensureActive()
            _imageEdit.commit()
         }
         .onFailure { error ->
            // Cancellation is control flow, not an expected persistence failure.
            if (error is CancellationException) throw error
            currentCoroutineContext().ensureActive()
         }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Use Case bündelt das Aktualisieren der Person mit dem Abschluss der
 *   laufenden Bildbearbeitung zu einer fachlich zusammengehörigen Operation.
 *
 * - Ein bisher gespeichertes Originalbild darf erst entfernt werden, nachdem
 *   der neue imagePath erfolgreich im Repository gespeichert wurde.
 *
 * Lernziele:
 *
 * - Mehrschrittige Anwendungslogik in einem Use Case kapseln.
 * - Persistierte Originaldaten bei fehlgeschlagenen Updates schützen.
 * - Use Cases unabhängig vom Android-UI-Lebenszyklus testen.
 */
