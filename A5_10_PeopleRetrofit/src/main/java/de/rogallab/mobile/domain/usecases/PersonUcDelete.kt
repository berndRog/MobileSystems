package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   // Deletes the server resource before removing its local image file.
   // This order prevents a failed request from leaving a broken image reference.
   suspend operator fun invoke(person: Person): Result<Unit> {
      val result = _repository.remove(person)

      if (result.isSuccess) {
         // Image cleanup is best effort because the person is already deleted.
         _imageFileStorage.deleteImageFromAppStorage(person.imagePath)
            .onFailure { throwable ->
               Alog.e(TAG, "delete person image failed: ${throwable.message}")
            }
      }

      return result
   }

   companion object {
      private const val TAG = "<-PersonUcDelete"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - "Person löschen" umfasst in A5_10 den Datensatz auf dem Server und die
 *   lokal verwaltete Bilddatei, deren Pfad lediglich als String übertragen wird.
 *
 * - Zuerst wird der Serverdatensatz gelöscht. Bei einem Netzwerkfehler bleibt
 *   das Bild erhalten und die Person besitzt weiterhin eine gültige Referenz.
 *
 * - Das anschließende Löschen der Datei ist Best Effort: Ein Dateifehler darf
 *   die bereits erfolgreiche Serveroperation nicht nachträglich als Fehler melden.
 *
 * Lernziele:
 *
 * - Entfernte und lokale Datenquellen in einem Use Case koordinieren.
 * - Nicht atomare Schritte in einer fachlich sicheren Reihenfolge ausführen.
 * - Hauptoperation und nachgelagertes Aufräumen unterscheiden.
 */
