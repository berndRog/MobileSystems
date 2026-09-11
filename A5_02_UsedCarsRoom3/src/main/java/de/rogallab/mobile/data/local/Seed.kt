package de.rogallab.mobile.data.local

import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.ImageFileFormat
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

         val person = PersonDto(firstName, lastName, email, phone, imagePath, uuidString)
         personDtos.add(person)
      }

      return personDtos as List<PersonDto>
   }

   suspend fun createCarDtos(): List<CarDto> {

      return  listOf(
         CarDto(
            id = FIAT_ID,
            manufacturer = "Fiat",
            model = "500",
            price = 18_900,
            personId = ARNE_ID,
            imagePaths = listOf(
               convertDrawable(R.drawable.car_fiat_500, "fiat_500_01"),
               convertDrawable(R.drawable.digit_2, "fiat_500_02"),
               convertDrawable(R.drawable.digit_3, "fiat_500_03"),
               convertDrawable(R.drawable.digit_4, "fiat_500_04"),
               convertDrawable(R.drawable.digit_5, "fiat_500_05"),
            ),
         ),
         CarDto(
            id = GOLF_ID,
            manufacturer = "Volkswagen",
            model = "Golf 8",
            price = 18_900,
            personId = ARNE_ID,
            imagePaths = listOf(
               convertDrawable(R.drawable.car_vw_golf, "vw_golf8_01"),
               convertDrawable(R.drawable.digit_2, "vw_golf8_02"),
               convertDrawable(R.drawable.digit_3, "vw_golf8_03"),
               convertDrawable(R.drawable.digit_4, "vw_golf8_04"),
               convertDrawable(R.drawable.digit_5, "vw_golf8_05"),
               convertDrawable(R.drawable.digit_6, "vw_golf8_06"),
               convertDrawable(R.drawable.digit_7, "vw_golf8_07"),
            ),
         ),
         CarDto(
            id = MOKKA_ID,
            manufacturer = "Opel",
            model = "Mokka-E",
            price = 20_500,
            personId = BERTA_ID,
            imagePaths = listOf(
               convertDrawable(R.drawable.car_opel_mokka, "opel_mokka_01"),
               convertDrawable(R.drawable.digit_2, "opel_mokka_02"),
               convertDrawable(R.drawable.digit_3, "opel_mokka_03"),
               convertDrawable(R.drawable.digit_4, "opel_mokka_04"),
               convertDrawable(R.drawable.digit_5, "opel_mokka_05"),
            ),
         ),
         CarDto(
            id = SEAT_ID,
            manufacturer = "Seat",
            model = "Ibiza TSI",
            price = 28_500,
            personId = CORD_ID,
            imagePaths = listOf(
               convertDrawable(R.drawable.car_seat_ibiza_2025, "seat_ibiza_01"),
               convertDrawable(R.drawable.digit_2, "seat_ibiza_02"),
               convertDrawable(R.drawable.digit_3, "seat_ibiza_03"),
               convertDrawable(R.drawable.digit_4, "seat_ibiza_04"),
            ),
         ),
         CarDto(
            id = CAPTUR_ID,
            manufacturer = "Renault",
            model = "CAPTUR E-Tech",
            price = 32_500,
            personId = DAGMAR_ID,
            imagePaths = listOf(
               convertDrawable(R.drawable.car_renault_captur, "renault_capture_01"),
               convertDrawable(R.drawable.digit_2, "renault_capture_02"),
               convertDrawable(R.drawable.digit_3, "renault_capture_03"),
               convertDrawable(R.drawable.digit_4, "renault_capture_04"),
               convertDrawable(R.drawable.digit_5, "renault_capture_05"),
               convertDrawable(R.drawable.digit_6, "renault_capture_06"),
               convertDrawable(R.drawable.digit_7, "renault_capture_07"),
               convertDrawable(R.drawable.digit_8, "renault_capture_08"),
            ),
         ),
         CarDto(
            id = E308_ID,
            manufacturer = "Peugot",
            model = "E308",
            price = 35_500,
            personId = DAGMAR_ID,
            imagePaths = listOf(
               convertDrawable(R.drawable.car_peugot_e308, "peugot_e308_01"),
               convertDrawable(R.drawable.digit_2, "peugot_e308_02"),
               convertDrawable(R.drawable.digit_3, "peugot_e308_03"),
               convertDrawable(R.drawable.digit_4, "peugot_e308_04"),
               convertDrawable(R.drawable.digit_5, "peugot_e308_05")
            ),
         ),
         CarDto(
            id = SCALA_ID,
            manufacturer = "Scoda",
            model = "Scala TSI",
            price = 35_500,
            personId = DAGMAR_ID,
            imagePaths = listOf(
               convertDrawable(R.drawable.car_skoda_scala, "scoda_scala_01"),
               convertDrawable(R.drawable.digit_2, "scoda_scala_02"),
               convertDrawable(R.drawable.digit_3, "scoda_scala_03"),
               convertDrawable(R.drawable.digit_4, "scoda_scala_04")
            ),
         )
      )

   }

   fun createsTestDriveDtos(): List<TDriveDto> {
      return  listOf(
         TDriveDto(
            id = TDRIVE_1_ID,
            personId = FRIEDA_ID,
            carId = FIAT_ID,
            start = "2026-08-04T14:00:00",
            isCompleted = false,
         ),
         TDriveDto(
            id = TDRIVE_2_ID,
            personId = FRIEDA_ID,
            carId = GOLF_ID,
            start = "2026-08-06T10:30:00",
            isCompleted = false,
         ),
         TDriveDto(
            id = TDRIVE_3_ID,
            personId = HANNA_ID,
            carId = FIAT_ID,
            start = "2026-08-07T16:00:00",
            isCompleted = true,
         )
      )
   }

   private suspend fun convertDrawable(drawableResId: Int, fileName: String): String {
      return _imageFileStorage.saveDrawableToAppStorage(
         drawableResId = drawableResId,
         fileName = fileName,
         format = ImageFileFormat.Png,
         quality = 90,
      ).getOrThrow()
   }

   private companion object {
      const val ARNE_ID = "01000000-0000-0000-0000-000000000000"
      const val BERTA_ID = "02000000-0000-0000-0000-000000000000"
      const val CORD_ID = "03000000-0000-0000-0000-000000000000"
      const val DAGMAR_ID = "04000000-0000-0000-0000-000000000000"
      const val ERNST_ID = "05000000-0000-0000-0000-000000000000"
      const val FRIEDA_ID = "06000000-0000-0000-0000-000000000000"
      const val HANNA_ID = "08000000-0000-0000-0000-000000000000"

      const val FIAT_ID = "00000000-0100-0000-0000-000000000000"
      const val GOLF_ID = "00000000-0200-0000-0000-000000000000"
      const val MOKKA_ID = "00000000-0300-0000-0000-000000000000"
      const val SEAT_ID = "00000000-0400-0000-0000-000000000000"
      const val CAPTUR_ID = "00000000-0500-0000-0000-000000000000"
      const val E308_ID = "00000000-0600-0000-0000-000000000000"
      const val SCALA_ID = "00000000-0700-0000-0000-000000000000"

      const val TDRIVE_1_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e03"
      const val TDRIVE_2_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e04"
      const val TDRIVE_3_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e05"
   }

}