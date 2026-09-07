package de.rogallab.mobile.data.local.database

import de.rogallab.mobile.R
import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.TDriveDto
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.ImageFileFormat
import java.util.Locale

class SeedDatabase(
   private val _personDao: IPersonDao,
   private val _carDao: ICarDao,
   private val _tDriveDao: ITDriveDao,
   private val _imageFileStorage: IImageFileStorage,
) {
   suspend fun seed() {
      if (_personDao.count() == 0)
         seedPeople()

      if (_carDao.count() == 0)
         seedCars()

      if (_tDriveDao.count() == 0)
         seedTestDrives()
   }

   private suspend fun seedPeople() {
      val names = listOf(
         "Arne" to "Arndt", "Berta" to "Bauer",
         "Cord" to "Conrad", "Dagmar" to "Diehl",
         "Ernst" to "Engel", "Frieda" to "Fischer",
         "Günter" to "Graf", "Hanna" to "Hoffmann",
         "Ingo" to "Imhoff", "Johanna" to "Jung",
         "Klaus" to "Klein", "Luise" to "Lang",
         "Martin" to "Meier", "Nadja" to "Neumann",
         "Otto" to "Olbrich", "Patrizia" to "Peters",
         "Quirin" to "Quart", "Rebecca" to "Richter",
         "Stefan" to "Schmidt", "Tanja" to "Thormann",
         "Uwe" to "Ulrich", "Veronika" to "Vogel",
         "Walter" to "Wagner", "Xenia" to "Xander",
         "Yannick" to "Yakov", "Zwantje" to "Zander",
      )

      val drawables = listOf(
         R.drawable.man_01, R.drawable.woman_01,
         R.drawable.man_02, R.drawable.woman_02,
         R.drawable.man_03, R.drawable.woman_03,
         R.drawable.man_04, R.drawable.woman_04,
         R.drawable.man_05, R.drawable.woman_05,
         R.drawable.man_06, R.drawable.woman_06,
         R.drawable.man_07, R.drawable.woman_07,
         R.drawable.man_08, R.drawable.woman_08,
         R.drawable.man_09, R.drawable.woman_09,
         R.drawable.man_10, R.drawable.woman_10,
         R.drawable.man_11, R.drawable.woman_11,
         R.drawable.man_12, R.drawable.woman_12,
         R.drawable.man_13, R.drawable.woman_13,
      )

      val people = names.mapIndexed { index, (firstName, lastName) ->
         val id = personId(index)
         val imagePath = _imageFileStorage.saveDrawableToAppStorage(
            drawableResId = drawables[index],
            fileName = id,
            format = ImageFileFormat.Jpeg,
            quality = 90,
         ).getOrThrow()

         PersonDto(
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = "${firstName.lowercase(Locale.ROOT)}." +
               "${lastName.lowercase(Locale.ROOT)}@example.org",
            phone = String.format(
               Locale.ROOT,
               "+49 511 555 %04d",
               index + 1,
            ),
            imagePath = imagePath,
         )
      }

      _personDao.insert(people)
   }

   private suspend fun seedCars() {

      val cars = listOf(
         CarDto(
            id = FIAT_ID,
            manufacturer = "Fiat",
            model = "500",
            year = 2023,
            price = 18_900,
            sellerId = ARNE_ID,
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
            year = 2022,
            price = 18_900,
            sellerId = ARNE_ID,
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
            year = 2022,
            price = 20_500,
            sellerId = BERTA_ID,
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
            year = 2024,
            price = 28_500,
            sellerId = CORD_ID,
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
            year = 2024,
            price = 32_500,
            sellerId = DAGMAR_ID,
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
            year = 2025,
            price = 35_500,
            sellerId = DAGMAR_ID,
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
            year = 2025,
            price = 35_500,
            sellerId = DAGMAR_ID,
            imagePaths = listOf(
               convertDrawable(R.drawable.car_peugot_e308, "peugot_e308_01"),
               convertDrawable(R.drawable.digit_2, "peugot_e308_02"),
               convertDrawable(R.drawable.digit_3, "peugot_e308_03"),
               convertDrawable(R.drawable.digit_4, "peugot_e308_04"),
               convertDrawable(R.drawable.digit_5, "peugot_e308_05")
            ),
         )
      )
      _carDao.insert(cars)

   }

   private suspend fun convertDrawable(drawableResId: Int, fileName: String): String {
      return _imageFileStorage.saveDrawableToAppStorage(
         drawableResId = drawableResId,
         fileName = fileName,
         format = ImageFileFormat.Png,
         quality = 90,
      ).getOrThrow()
   }

   private suspend fun seedTestDrives() {
      _tDriveDao.insert(
         TDriveDto(
            id = TDRIVE_1_ID,
            personId = FRIEDA_ID,
            carId = FIAT_ID,
            start = "2026-08-04T14:00:00",
            isCompleted = false,
         )
      )
      _tDriveDao.insert(
         TDriveDto(
            id = TDRIVE_2_ID,
            personId = FRIEDA_ID,
            carId = GOLF_ID,
            start = "2026-08-06T10:30:00",
            isCompleted = false,
         )
      )
      _tDriveDao.insert(
         TDriveDto(
            id = TDRIVE_3_ID,
            personId = HANNA_ID,
            carId = FIAT_ID,
            start = "2026-08-07T16:00:00",
            isCompleted = true,
         )
      )
   }

   private fun personId(index: Int): String =
      String.format(
         Locale.ROOT,
         "%02d000000-0000-0000-0000-000000000000",
         index + 1,
      )


   private companion object {
      const val ARNE_ID   = "01000000-0000-0000-0000-000000000000"
      const val BERTA_ID  = "02000000-0000-0000-0000-000000000000"
      const val CORD_ID   = "03000000-0000-0000-0000-000000000000"
      const val DAGMAR_ID = "04000000-0000-0000-0000-000000000000"
      const val ERNST_ID  = "05000000-0000-0000-0000-000000000000"
      const val FRIEDA_ID = "06000000-0000-0000-0000-000000000000"
      const val HANNA_ID  = "08000000-0000-0000-0000-000000000000"

      const val FIAT_ID   = "00000000-0100-0000-0000-000000000000"
      const val GOLF_ID   = "00000000-0200-0000-0000-000000000000"
      const val MOKKA_ID  = "00000000-0300-0000-0000-000000000000"
      const val SEAT_ID   = "00000000-0400-0000-0000-000000000000"
      const val CAPTUR_ID = "00000000-0500-0000-0000-000000000000"
      const val E308_ID   = "00000000-0600-0000-0000-000000000000"
      const val SCALA_ID  = "00000000-0700-0000-0000-000000000000"


      const val TDRIVE_1_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e03"
      const val TDRIVE_2_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e04"
      const val TDRIVE_3_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e05"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die Personen verwenden dieselben Namen, stabilen IDs und Bilder wie die
 *   vorherigen Beispiele. Dadurch bleibt der bekannte Datenbestand erhalten.
 *
 * - Personenbilder sind Drawables der App. Beim Seeding werden sie mit
 *   IImageFileStorage in den privaten App-Speicher kopiert und Room speichert
 *   anschließend den absoluten Dateipfad.
 *
 * - Fahrzeuge besitzen mehrere Bildreferenzen. Die Seed-Fotos stammen aus
 *   Wikimedia Commons; Front-, Heck- und Innenraumansicht zeigen die Arbeit
 *   mit List<String> und dem zugehörigen Room-Column-Converter.
 *
 * - Zwei Fahrzeuge gehören Arne und zwei Berta. Die 1:n-Beziehung zwischen
 *   Person und Car lässt sich damit direkt in den Beispieldaten erkennen.
 *
 * - Die drei Probefahrten bilden beide Richtungen einer m:n-Beziehung ab:
 *   Frieda fährt mehrere Fahrzeuge und der Golf wird von mehreren Personen
 *   probegefahren.
 */
