package de.rogallab.mobile.data.local

import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.ImageFileFormat
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import java.util.Locale
import kotlin.random.Random

class Seed(
   private val _imageFileStorage: IImageFileStorage,
   private val _withImages: Boolean = true
) : KoinComponent {

   var people: MutableList<Person> = mutableListOf<Person>()

   suspend fun createPeopleList() {
      val firstNames = mutableListOf(
         "Arne", "Berta", "Cord", "Dagmar", "Ernst", "Frieda", "Günter", "Hanna",
         "Ingo", "Johanna", "Klaus", "Luise", "Martin", "Nadja", "Otto", "Patrizia",
         "Quirin", "Rebecca", "Stefan", "Tanja", "Uwe", "Veronika", "Walter", "Xenia",
         "Yannick", "Zwantje")
      val lastNames = mutableListOf(
         "Arndt", "Bauer", "Conrad", "Diehl", "Engel", "Fischer", "Graf", "Hoffmann",
         "Imhoff", "Jung", "Klein", "Lang", "Meier", "Neumann", "Olbrich", "Peters",
         "Quart", "Richter", "Schmidt", "Thormann", "Ulrich", "Vogel", "Wagner", "Xander",
         "Yakov", "Zander")
      val emailProvider = mutableListOf("gmail.com", "icloud.com", "outlook.com", "yahoo.com",
         "t-online.de", "gmx.de", "freenet.de", "mailbox.org", "yahoo.com", "web.de")
      val random = Random(0)
      for (index in firstNames.indices) {
//         var indexFirst = random.nextInt(firstNames.size)
//         var indexLast = random.nextInt(lastNames.size)
         val firstName = firstNames[index]
         val lastName = lastNames[index]

         val email = sanitizeEmailInput(
            "${firstName.lowercase(locale = Locale.ROOT)}." +
            "${lastName.lowercase(locale = Locale.ROOT)}@" +
            "${emailProvider.random()}")

         val phone: String = sanitizePhoneInput(
            "0${random.nextInt(1234, 9999)} " +
               "${random.nextInt(100, 999)}-" +
               "${random.nextInt(10, 9999)}")

         val uuid = String.format(Locale.ROOT, "%02d000000-0000-0000-0000-000000000000", index + 1)
         val person = Person(firstName, lastName, email, phone, null, uuid)
         people.add(person)
      }

      // convert the drawables into image files
      if (_withImages) runBlocking { createImages() }
   }

   private suspend fun createImages() {
      val drawables = listOf(
         R.drawable.man_01, R.drawable.woman_01, R.drawable.man_02, R.drawable.woman_02,
         R.drawable.man_03, R.drawable.woman_03, R.drawable.man_04, R.drawable.woman_04,
         R.drawable.man_05, R.drawable.woman_05, R.drawable.man_06, R.drawable.woman_06,
         R.drawable.man_07, R.drawable.woman_07, R.drawable.man_08, R.drawable.woman_08,
         R.drawable.man_09, R.drawable.woman_09, R.drawable.man_10, R.drawable.woman_10,
         R.drawable.man_11, R.drawable.woman_11, R.drawable.man_12, R.drawable.woman_12,
         R.drawable.man_13, R.drawable.woman_13
      )

      check(people.size >= drawables.size) {
         "Not enough people for ${drawables.size} seed images."
      }

      drawables.forEachIndexed { index, drawableId ->

         val uuidString = String.format(
            Locale.ROOT, "%02d000000-0000-0000-0000-000000000000", index + 1)

         val imagePath = _imageFileStorage.saveDrawableToAppStorage(
            drawableResId = drawableId,
            fileName = uuidString,
            format = ImageFileFormat.Jpeg,
            quality = 90,
         )
         .getOrElse { throwable ->
            val message = throwable.localizedMessage
               ?: "Failed to create seed image: $uuidString"
            Alog.e("<-Seed", message)
            throw throwable
         }
         Alog.d("<-Seed", "Uri: $imagePath")

         // Update the person with the image path
         people[index] = people[index].copy(imagePath = imagePath)

      }
   }
}