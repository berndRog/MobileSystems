package de.rogallab.mobile.domain.usecases

import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

class PersonUcDelete(
   private val _repository: IPersonRepository,
   private val _imageFileStorage: IImageFileStorage,
) {

   // Deletes the database entry before removing its image file.
   // This order prevents a failed repository call from leaving a broken image reference.
   suspend operator fun invoke(person: Person): Result<Unit> {
      val result = _repository.remove(person)

      if (result.isSuccess) {
         // Image cleanup is best effort because the person has already been deleted.
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
 * - "Person löschen" umfasst auch im UsedCars-Modul den Room-Datensatz und
 *   die zugehörige Datei im privaten App-Speicher.
 *
 * - Room schützt Personen mit verknüpften Cars oder TestDrives durch
 *   ForeignKey.RESTRICT. Schlägt das Löschen deshalb fehl, wird das weiterhin
 *   referenzierte Personenbild nicht entfernt.
 *
 * - Erst nach erfolgreicher Datenbanklöschung räumt der Use Case die Bilddatei
 *   auf. Ein reiner Cleanup-Fehler ändert nicht nachträglich das Ergebnis der
 *   bereits abgeschlossenen Personenlöschung.
 *
 * Lernziele:
 *
 * - Use Cases mit relationalen Datenbankregeln kombinieren.
 * - Die Reihenfolge nicht-atomarer Schritte anhand möglicher Fehler begründen.
 * - Referentielle Integrität und Dateiverwaltung gemeinsam berücksichtigen.
 */
