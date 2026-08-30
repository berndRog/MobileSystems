package de.rogallab.mobile.data.local.database

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.TDriveDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SeedDatabase(
   private val _personDao: IPersonDao,
   private val _carDao: ICarDao,
   private val _tDriveDao: ITDriveDao,
   private val _applicationScope: CoroutineScope,
) {
   fun start() {
      _applicationScope.launch {
         seedPeople()
         seedCars()
         seedTestDrives()
      }
   }

   private suspend fun seedPeople() {
      if (_personDao.count() != 0) return
      _personDao.insert(
         listOf(
            PersonDto(
               id = SELLER_ANNA_ID,
               firstName = "Anna",
               lastName = "Schulz",
               email = "anna.schulz@example.org",
               phone = "+49 511 555 0101",
               imagePath = null,
            ),
            PersonDto(
               id = SELLER_BEN_ID,
               firstName = "Ben",
               lastName = "Meyer",
               email = "ben.meyer@example.org",
               phone = "+49 511 555 0102",
               imagePath = null,
            ),
            PersonDto(
               id = INTERESTED_CLARA_ID,
               firstName = "Clara",
               lastName = "Neumann",
               email = "clara.neumann@example.org",
               phone = "+49 511 555 0103",
               imagePath = null,
            ),
         )
      )
   }

   private suspend fun seedCars() {
      if (_carDao.count() != 0) return
      _carDao.insert(
         listOf(
            CarDto(
               id = VW_GOLF_ID,
               manufacturer = "Volkswagen",
               model = "Golf VIII",
               registrationYear = 2021,
               mileage = 42500,
               priceInEuro = 21900,
               sellerId = SELLER_ANNA_ID,
               imagePaths = emptyList(),
            ),
            CarDto(
               id = SKODA_OCTAVIA_ID,
               manufacturer = "Škoda",
               model = "Octavia Combi",
               registrationYear = 2020,
               mileage = 68900,
               priceInEuro = 18900,
               sellerId = SELLER_BEN_ID,
               imagePaths = emptyList(),
            ),
         )
      )
   }

   private suspend fun seedTestDrives() {
      if (_tDriveDao.count() != 0) return
      _tDriveDao.insert(
         listOf(
            TDriveDto(
               id = "50df1fbc-c915-4ce4-8d3e-168fe3013e03",
               personId = INTERESTED_CLARA_ID,
               carId = VW_GOLF_ID,
               start = "2026-08-04T14:00:00",
               notes = "Führerschein prüfen",
               isCompleted = false,
            )
         )
      )
   }

   private companion object {
      const val SELLER_ANNA_ID = "4ad4894d-014e-4566-942d-41315b91a12c"
      const val SELLER_BEN_ID = "f3952d14-88cf-4fa2-9ca7-71c63c739d97"
      const val INTERESTED_CLARA_ID = "805ee8ec-8984-44da-b6e3-9fba3e4c3654"
      const val VW_GOLF_ID = "795937c2-f61a-4fd8-9343-d2280140e1e1"
      const val SKODA_OCTAVIA_ID = "44a39e30-eb2e-4095-a2f2-6744fa1d12d0"
   }
}
