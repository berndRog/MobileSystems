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
      _carDao.insert(
         listOf(
            CarDto(
               id = GOLF_ID,
               manufacturer = "Volkswagen",
               model = "Golf VIII",
               registrationYear = 2021,
               mileage = 42_500,
               priceInEuro = 21_900,
               sellerId = ARNE_ID,
               imagePaths = listOf(
                  GOLF_FRONT,
                  GOLF_REAR,
                  GOLF_INTERIOR,
               ),
            ),
            CarDto(
               id = OCTAVIA_ID,
               manufacturer = "Škoda",
               model = "Octavia IV",
               registrationYear = 2020,
               mileage = 68_900,
               priceInEuro = 18_900,
               sellerId = BERTA_ID,
               imagePaths = listOf(
                  OCTAVIA_FRONT,
                  OCTAVIA_REAR,
                  OCTAVIA_INTERIOR,
               ),
            ),
            CarDto(
               id = ASTRA_ID,
               manufacturer = "Opel",
               model = "Astra L",
               registrationYear = 2022,
               mileage = 31_400,
               priceInEuro = 20_500,
               sellerId = CORD_ID,
               imagePaths = listOf(
                  ASTRA_FRONT,
                  ASTRA_REAR,
                  ASTRA_INTERIOR,
               ),
            ),
            CarDto(
               id = BMW_ID,
               manufacturer = "BMW",
               model = "320d xDrive M Sport",
               registrationYear = 2019,
               mileage = 76_300,
               priceInEuro = 24_900,
               sellerId = ARNE_ID,
               imagePaths = listOf(
                  BMW_FRONT,
                  BMW_REAR,
                  BMW_INTERIOR,
               ),
            ),
            CarDto(
               id = FOCUS_ID,
               manufacturer = "Ford",
               model = "Focus ST-Line",
               registrationYear = 2018,
               mileage = 91_200,
               priceInEuro = 13_900,
               sellerId = BERTA_ID,
               imagePaths = listOf(
                  FOCUS_FRONT,
                  FOCUS_REAR,
                  FOCUS_INTERIOR,
               ),
            ),
         )
      )
   }

   private suspend fun seedTestDrives() {
      _tDriveDao.insert(
         TDriveDto(
            id = TDRIVE_1_ID,
            personId = FRIEDA_ID,
            carId = GOLF_ID,
            start = "2026-08-04T14:00:00",
            notes = "Führerschein prüfen",
            isCompleted = false,
         )
      )
      _tDriveDao.insert(
         TDriveDto(
            id = TDRIVE_2_ID,
            personId = FRIEDA_ID,
            carId = OCTAVIA_ID,
            start = "2026-08-06T10:30:00",
            notes = "Kombi vergleichen",
            isCompleted = false,
         )
      )
      _tDriveDao.insert(
         TDriveDto(
            id = TDRIVE_3_ID,
            personId = HANNA_ID,
            carId = GOLF_ID,
            start = "2026-08-07T16:00:00",
            notes = "Probefahrt Golf",
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
      const val ARNE_ID = "01000000-0000-0000-0000-000000000000"
      const val BERTA_ID = "02000000-0000-0000-0000-000000000000"
      const val CORD_ID = "03000000-0000-0000-0000-000000000000"
      const val FRIEDA_ID = "06000000-0000-0000-0000-000000000000"
      const val HANNA_ID = "08000000-0000-0000-0000-000000000000"

      const val GOLF_ID = "795937c2-f61a-4fd8-9343-d2280140e1e1"
      const val OCTAVIA_ID = "44a39e30-eb2e-4095-a2f2-6744fa1d12d0"
      const val ASTRA_ID = "35000000-0000-0000-0000-000000000001"
      const val BMW_ID = "35000000-0000-0000-0000-000000000002"
      const val FOCUS_ID = "35000000-0000-0000-0000-000000000003"

      const val TDRIVE_1_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e03"
      const val TDRIVE_2_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e04"
      const val TDRIVE_3_ID = "50df1fbc-c915-4ce4-8d3e-168fe3013e05"

      const val GOLF_FRONT =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "Volkswagen%20Golf%20VIII%20IMG%203381.jpg?width=1280"
      const val GOLF_REAR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "Volkswagen%20Golf%20VIII%20IMG%202052.jpg?width=1280"
      const val GOLF_INTERIOR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "Volkswagen%20Golf%20VIII%20-%20Life%201st%20-%20" +
            "Int%C3%A9rieur.jpg?width=1280"

      const val OCTAVIA_FRONT =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2021%20Skoda%20Octavia%20SE%20First%20Edition%20TSi%20" +
            "e-TEC%20SA%201.0%20Front.jpg?width=1280"
      const val OCTAVIA_REAR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2021%20Skoda%20Octavia%20SE%20First%20Edition%20TSi%20" +
            "e-TEC%20SA%201.0%20Rear.jpg?width=1280"
      const val OCTAVIA_INTERIOR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "Skoda%20Octavia%20IV%20interior.jpg?width=1280"

      const val ASTRA_FRONT =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "Opel%20Astra%20L%201X7A0336.jpg?width=1280"
      const val ASTRA_REAR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "Opel%20Astra%20L%201X7A0338.jpg?width=1280"
      const val ASTRA_INTERIOR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2022%20Vauxhall%20Astra%20GS-Line%201.2%20" +
            "%28Interior%29.jpg?width=1280"

      const val BMW_FRONT =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2019%20BMW%20320d%20xDrive%20M%20Sport%202.0%20" +
            "Front.jpg?width=1280"
      const val BMW_REAR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2019%20BMW%20320d%20xDrive%20M%20Sport%202.0%20" +
            "Rear.jpg?width=1280"
      const val BMW_INTERIOR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2019%20BMW%20320d%20xDrive%20M%20Sport%202.0%20" +
            "Interior.jpg?width=1280"

      const val FOCUS_FRONT =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2018%20Ford%20Focus%20ST-Line%20Front.jpg?width=1280"
      const val FOCUS_REAR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2018%20Ford%20Focus%20ST-Line%20Rear.jpg?width=1280"
      const val FOCUS_INTERIOR =
         "https://commons.wikimedia.org/wiki/Special:Redirect/file/" +
            "2018%20Ford%20Focus%20ST-Line%20X%20Interior.jpg?width=1280"
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
