package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.images.IImageEdit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException

class PersonUcCreate(
   private val _repository: IPersonRepository,
   private val _imageEdit: IImageEdit,
) {

   suspend operator fun invoke(person: Person): Result<Unit> {
      currentCoroutineContext().ensureActive()
      return _repository.create(person)
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
 * - Der Use Case beschreibt die vollständige Anwendungsoperation
 *   "Person mit Bild anlegen" und koordiniert dafür Repository und ImageEdit.
 *
 * - Die Bild-Session wird erst nach einem erfolgreichen Repository-Zugriff
 *   übernommen. Bei einem Fehler kann der Benutzer den Speichervorgang erneut
 *   versuchen oder die Bearbeitung abbrechen.
 *
 * Lernziele:
 *
 * - Zusammengesetzte Anwendungslogik aus dem ViewModel auslagern.
 * - Technische Abhängigkeiten über Constructor Injection bereitstellen.
 * - Die Reihenfolge von Datenbankzugriff und Bildverwaltung begründen.
 */
