package de.rogallab.mobile.data.local.database

import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.TDriveDto

class SeedDatabase(
   private val _personDao: IPersonDao,
   private val _carDao: ICarDao,
   private val _tDriveDao: ITDriveDao,
) {
   suspend fun seed() {
      if (_personDao.count() == 0) {
         _personDao.insert(listOf(
            PersonDto(ANNA_ID, "Anna", "Schulz", "anna.schulz@example.org", "+49 511 555 0101", null),
            PersonDto(BEN_ID, "Ben", "Meyer", "ben.meyer@example.org", "+49 511 555 0102", null),
            PersonDto(CLARA_ID, "Clara", "Neumann", "clara.neumann@example.org", "+49 511 555 0103", null),
         ))
      }
      if (_carDao.count() == 0) {
         _carDao.insert(listOf(
            CarDto(GOLF_ID, "Volkswagen", "Golf VIII", 2021, 42500, 21900, ANNA_ID),
            CarDto(OCTAVIA_ID, "Škoda", "Octavia Combi", 2020, 68900, 18900, BEN_ID),
         ))
      }
      if (_tDriveDao.count() == 0) {
         _tDriveDao.insert(TDriveDto(
            id = "50df1fbc-c915-4ce4-8d3e-168fe3013e03",
            personId = CLARA_ID,
            carId = GOLF_ID,
            start = "2026-08-04T14:00:00",
            notes = "Führerschein prüfen",
            isCompleted = false,
         ))
      }
   }

   private companion object {
      const val ANNA_ID = "4ad4894d-014e-4566-942d-41315b91a12c"
      const val BEN_ID = "f3952d14-88cf-4fa2-9ca7-71c63c739d97"
      const val CLARA_ID = "805ee8ec-8984-44da-b6e3-9fba3e4c3654"
      const val GOLF_ID = "795937c2-f61a-4fd8-9343-d2280140e1e1"
      const val OCTAVIA_ID = "44a39e30-eb2e-4095-a2f2-6744fa1d12d0"
   }
}
