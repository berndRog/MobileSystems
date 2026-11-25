package de.rogallab.mobile.data.local

import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.ImageFileFormat
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput
import org.koin.core.component.KoinComponent
import java.util.Locale
import kotlin.random.Random

class Seed(
   private val _imageFileStorage: IImageFileStorage
) : KoinComponent {

   suspend fun createPersonDtos(): List<PersonDto> {

      var personDtos: MutableList<PersonDto> = mutableListOf<PersonDto>()

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

      val drawables = listOf(
         R.drawable.man_01, R.drawable.woman_01, R.drawable.man_02, R.drawable.woman_02,
         R.drawable.man_03, R.drawable.woman_03, R.drawable.man_04, R.drawable.woman_04,
         R.drawable.man_05, R.drawable.woman_05, R.drawable.man_06, R.drawable.woman_06,
         R.drawable.man_07, R.drawable.woman_07, R.drawable.man_08, R.drawable.woman_08,
         R.drawable.man_09, R.drawable.woman_09, R.drawable.man_10, R.drawable.woman_10,
         R.drawable.man_11, R.drawable.woman_11, R.drawable.man_12, R.drawable.woman_12,
         R.drawable.man_13, R.drawable.woman_13
      )

      drawables.forEachIndexed { index, drawableId ->

         val firstName = firstNames[index]
         val lastName = lastNames[index]

         val provider = emailProvider[index % emailProvider.size]   // rotiert bei Überlauf wieder von vorne
         val email = sanitizeEmailInput(
            "${firstName.lowercase(locale = Locale.ROOT)}." +
               "${lastName.lowercase(locale = Locale.ROOT)}@$provider")

         val phone: String = sanitizePhoneInput(
            "0${random.nextInt(1234, 9999)} " +
               "${random.nextInt(100, 999)}-" +
               "${random.nextInt(10, 9999)}")

         val uuidString = String.format(
            Locale.ROOT, "%02d000000-0000-0000-0000-000000000000", index + 1)

         val imagePath = _imageFileStorage.saveDrawableToAppStorage(
            drawableResId = drawableId,
            fileName = uuidString,
            format = ImageFileFormat.Jpeg,
            quality = 90,
         ).getOrElse { throwable ->
            val message = throwable.localizedMessage
               ?: "Failed to create seed image: $uuidString"
            throw throwable
         }

         val person = PersonDto(
            id = uuidString,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            imagePath = imagePath
         )
         personDtos.add(person)
      }

      return personDtos as List<PersonDto>
   }

}
