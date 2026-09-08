package de.rogallab.mobile.data.local

import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.ImageFileFormat
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
import java.util.Locale
import kotlin.random.Random

class Seed(
   private val _imageFileStorage: IImageFileStorage,
   private val _withImages: Boolean = true,
) {

   val people: MutableList<Person> = mutableListOf()

   suspend fun createPeopleList() {
      people.clear()

      val firstNames = listOf(
         "Arne", "Berta", "Cord", "Dagmar", "Ernst", "Frieda", "Günter", "Hanna",
         "Ingo", "Johanna", "Klaus", "Luise", "Martin", "Nadja", "Otto", "Patrizia",
         "Quirin", "Rebecca", "Stefan", "Tanja", "Uwe", "Veronika", "Walter", "Xenia",
         "Yannick", "Zwantje",
      )
      val lastNames = listOf(
         "Arndt", "Bauer", "Conrad", "Diehl", "Engel", "Fischer", "Graf", "Hoffmann",
         "Imhoff", "Jung", "Klein", "Lang", "Meier", "Neumann", "Olbrich", "Peters",
         "Quart", "Richter", "Schmidt", "Thormann", "Ulrich", "Vogel", "Wagner", "Xander",
         "Yakov", "Zander",
      )
      val emailProviders = listOf(
         "t-online.de", "gmx.de", "web.de", "freenet.de",
         "posteo.de", "mailbox.org", "tuta.com", "mail.de",
      )

      val random = Random(0)
      for (index in firstNames.indices) {
         val firstName = firstNames[index]
         val lastName = lastNames[index]
         val provider = emailProviders[index % emailProviders.size]

         val email = sanitizeEmailInput(
            "${firstName.lowercase(Locale.ROOT)}." +
               "${lastName.lowercase(Locale.ROOT)}@$provider"
         )
         val phone = sanitizePhoneInput(
            "0${random.nextInt(1234, 9999)} " +
               "${random.nextInt(100, 999)}-" +
               "${random.nextInt(10, 9999)}"
         )
         val id = String.format(
            Locale.ROOT,
            "%02d000000-0000-0000-0000-000000000000",
            index + 1,
         )

         people.add(
            Person(
               firstName = firstName,
               lastName = lastName,
               email = email,
               phone = phone,
               imagePath = null,
               id = id,
            )
         )
      }

      if (_withImages)
         createImages()
   }

   suspend fun deleteLocalImages() {
      people.forEach { person: Person ->
         _imageFileStorage
            .deleteImageFromAppStorage(person.imagePath)
            .onFailure { throwable ->
               Alog.e(TAG, "delete image: ${throwable.message}")
            }
      }
   }

   private suspend fun createImages() {
      val drawables = listOf(
         R.drawable.man_01, R.drawable.woman_01, R.drawable.man_02, R.drawable.woman_02,
         R.drawable.man_03, R.drawable.woman_03, R.drawable.man_04, R.drawable.woman_04,
         R.drawable.man_05, R.drawable.woman_05, R.drawable.man_06, R.drawable.woman_06,
         R.drawable.man_07, R.drawable.woman_07, R.drawable.man_08, R.drawable.woman_08,
         R.drawable.man_09, R.drawable.woman_09, R.drawable.man_10, R.drawable.woman_10,
         R.drawable.man_11, R.drawable.woman_11, R.drawable.man_12, R.drawable.woman_12,
         R.drawable.man_13, R.drawable.woman_13,
      )

      check(people.size >= drawables.size) {
         "Not enough people for ${drawables.size} seed images."
      }

      drawables.forEachIndexed { index, drawableId ->
         val id = String.format(
            Locale.ROOT,
            "%02d000000-0000-0000-0000-000000000000",
            index + 1,
         )

         val imagePath = _imageFileStorage.saveDrawableToAppStorage(
            drawableResId = drawableId,
            fileName = id,
            format = ImageFileFormat.Jpeg,
            quality = 90,
         ).getOrElse { throwable ->
            Alog.e(TAG, "create image: ${throwable.message}")
            throw throwable
         }

         people[index] = people[index].copy(imagePath = imagePath)
      }
   }

   companion object {
      private const val TAG = "<-Seed"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Seed erzeugt dieselben deterministischen 26 Personen wie die lokalen Beispiele,
 *   damit Room- und Retrofit-Variante direkt vergleichbar bleiben.
 * - Die Seed-Daten entstehen jetzt im Android-Client. Die PeopleApi muss keine
 *   fachlichen Beispieldaten mehr kennen oder beim Start automatisch anlegen.
 * - Optional werden die vorhandenen Drawable-Portraits in temporäre lokale JPEG-
 *   Dateien umgewandelt. SeedApi kann diese anschließend per Multipart hochladen.
 * - Nach erfolgreichem Upload werden die lokalen Seed-Dateien wieder gelöscht;
 *   persistente Bilder gehören in A5_11 ausschließlich dem Server.
 */
