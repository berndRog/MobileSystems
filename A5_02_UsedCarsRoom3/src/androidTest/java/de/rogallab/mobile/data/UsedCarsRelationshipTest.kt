package de.rogallab.mobile.data

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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
   private lateinit var database: AppDatabase
   private lateinit var personDao: IPersonDao
   private lateinit var carDao: ICarDao
   private lateinit var tDriveDao: ITDriveDao

   @Before fun setup() {
      database = Room.inMemoryDatabaseBuilder(
         ApplicationProvider.getApplicationContext(), AppDatabase::class.java,
      ).setDriver(BundledSQLiteDriver()).allowMainThreadQueries().build()
      personDao = database.createPersonDao()
      carDao = database.createCarDao()
      tDriveDao = database.createTDriveDao()
   }
   @After fun tearDown() { database.close() }

   @Test fun findWithCars_returnsOneToManyMultimap() = runTest {
      val seller = person("seller", "Anna", "Schulz")
      val first = car("car-1", seller.id, "Golf")
      val second = car("car-2", seller.id, "Passat")
      personDao.insert(seller)
      carDao.insert(listOf(first, second))

      val relation = personDao.findWithCars(seller.id)

      assertEquals(setOf(seller), relation.keys)
      assertEquals(
         setOf(first.id, second.id),
         relation[seller]?.map(CarDto::id)?.toSet(),
      )
   }

   @Test fun findWithTestDriveCars_returnsManyToManyMultimap() = runTest {
      val seller = person("seller", "Anna", "Schulz")
      val interested = person("interested", "Clara", "Neumann")
      val first = car("car-1", seller.id, "Golf")
      val second = car("car-2", seller.id, "Passat")
      personDao.insert(listOf(seller, interested))
      carDao.insert(listOf(first, second))
      tDriveDao.insert(listOf(
         drive("td-1", interested.id, first.id, "2026-08-04T14:00:00"),
         drive("td-2", interested.id, second.id, "2026-08-05T15:00:00"),
      ))

      val relation = personDao.findWithTestDriveCars(interested.id)

      assertEquals(setOf(interested), relation.keys)
      assertEquals(
         setOf(first.id, second.id),
         relation[interested]?.map(CarDto::id)?.toSet(),
      )
   }

   private fun person(id: String, first: String, last: String) =
      PersonDto(id, first, last, null, null, null)
   private fun car(id: String, sellerId: String, model: String) =
      CarDto(id, "Volkswagen", model, 2021, 42500, 21900, sellerId)
   private fun drive(id: String, personId: String, carId: String, start: String) =
      TDriveDto(id, personId, carId, start, null, false)
}
