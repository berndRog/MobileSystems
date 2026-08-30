package de.rogallab.mobile.data

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.TDriveDto
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsedCarsRelationshipTest {
   private lateinit var _database: AppDatabase
   private lateinit var _personDao: IPersonDao
   private lateinit var _carDao: ICarDao
   private lateinit var _tDriveDao: ITDriveDao

   @Before
   fun setup() {
      _database = Room.inMemoryDatabaseBuilder(
         ApplicationProvider.getApplicationContext(),
         AppDatabase::class.java,
      )
         .setDriver(AndroidSQLiteDriver())
         .allowMainThreadQueries()
         .build()

      _personDao = _database.createPersonDao()
      _carDao = _database.createCarDao()
      _tDriveDao = _database.createTDriveDao()
   }

   @After
   fun tearDown() {
      _database.close()
   }

   @Test
   fun findWithCars_returnsOneToManyRelationship() = runTest {
      val seller = createSeller()
      val firstCar = createCar(
         id = "car-1",
         sellerId = seller.id,
         model = "Golf",
      )
      val secondCar = createCar(
         id = "car-2",
         sellerId = seller.id,
         model = "Passat",
      )

      _personDao.insert(seller)
      _carDao.insert(listOf(firstCar, secondCar))

      val relation = _personDao.findWithCars(seller.id)

      assertEquals(seller.id, relation?.person?.id)
      assertEquals(
         setOf(firstCar.id, secondCar.id),
         relation?.cars?.map(CarDto::id)?.toSet(),
      )
   }

   @Test
   fun findWithTestDriveCars_returnsManyToManyRelationship() = runTest {
      val seller = createSeller()
      val interestedPerson = createInterestedPerson()
      val firstCar = createCar(
         id = "car-1",
         sellerId = seller.id,
         model = "Golf",
      )
      val secondCar = createCar(
         id = "car-2",
         sellerId = seller.id,
         model = "Passat",
      )

      _personDao.insert(listOf(seller, interestedPerson))
      _carDao.insert(listOf(firstCar, secondCar))
      _tDriveDao.insert(
         listOf(
            createTestDrive(
               id = "test-drive-1",
               personId = interestedPerson.id,
               carId = firstCar.id,
               start = "2026-08-04T14:00:00",
            ),
            createTestDrive(
               id = "test-drive-2",
               personId = interestedPerson.id,
               carId = secondCar.id,
               start = "2026-08-05T15:00:00",
            ),
         )
      )

      val relation =
         _personDao.findWithTestDriveCars(interestedPerson.id)

      assertEquals(interestedPerson.id, relation?.person?.id)
      assertEquals(
         setOf(firstCar.id, secondCar.id),
         relation?.cars?.map(CarDto::id)?.toSet(),
      )
   }

   private fun createSeller(): PersonDto = PersonDto(
      id = "seller",
      firstName = "Anna",
      lastName = "Schulz",
      email = null,
      phone = null,
      imagePath = null,
   )

   private fun createInterestedPerson(): PersonDto = PersonDto(
      id = "interested",
      firstName = "Clara",
      lastName = "Neumann",
      email = null,
      phone = null,
      imagePath = null,
   )

   private fun createCar(
      id: String,
      sellerId: String,
      model: String,
   ): CarDto = CarDto(
      id = id,
      manufacturer = "Volkswagen",
      model = model,
      registrationYear = 2021,
      mileage = 42_500,
      priceInEuro = 21_900,
      sellerId = sellerId,
   )

   private fun createTestDrive(
      id: String,
      personId: String,
      carId: String,
      start: String,
   ): TDriveDto = TDriveDto(
      id = id,
      personId = personId,
      carId = carId,
      start = start,
      notes = null,
      isCompleted = false,
   )
}
