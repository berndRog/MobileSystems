package de.rogallab.mobile.data.local.database

import android.app.Application
import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.local.dtos.PersonDto
import de.rogallab.mobile.data.local.dtos.TDriveDto
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class RoomRelationsTest {
   @Test
   fun sellerCarsAndTestDriveCars_followTheirSeparateRelations() = runTest {
      val context = ApplicationProvider.getApplicationContext<android.content.Context>()
      val database = Room.inMemoryDatabaseBuilder<AppDatabase>(context)
         .setDriver(AndroidSQLiteDriver())
         .build()
      try {
         val seller = PersonDto("seller", "Ada", "Lovelace", null, null, null)
         val driver = PersonDto("driver", "Grace", "Hopper", null, null, null)
         val car = CarDto("car", "Ford", "Focus", 10000, emptyList(), seller.id)
         database.createPersonDao().insert(listOf(seller, driver))
         database.createCarDao().insert(car)
         database.createTDriveDao().insert(
            TDriveDto("drive", driver.id, car.id, Instant.parse("2026-01-01T12:00:00Z"), false)
         )

         assertEquals(listOf(car), database.createPersonDao().findByIdWithCars(seller.id)[seller])
         assertTrue(database.createPersonDao().findByIdWithCars(driver.id).isEmpty())
         assertEquals(listOf(car), database.createPersonDao().findWithTestDriveCars(driver.id)[driver])
         assertTrue(database.createPersonDao().findWithTestDriveCars(seller.id).isEmpty())
      } finally {
         database.close()
      }
   }
}
