package de.rogallab.mobile.data.repositories

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.data.remote.dtos.TDriveDto
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.testing.FakeTDriveWebservice
import kotlin.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class TDriveRepositoryTest {
   private val webservice = FakeTDriveWebservice().apply {
      drives = listOf(
         drive("completed", "2026-11-03T10:00:00Z", completed = true),
         drive("later", "2026-11-04T10:00:00Z"),
         drive("earlier", "2026-11-02T10:00:00Z"),
      )
   }
   private val repository = TDriveRepository(
      _webservice = webservice,
      _networkExceptionMapper = NetworkExceptionMapper(ApplicationProvider.getApplicationContext()),
   )

   @Test fun observeAllLoadsAndSortsOpenDrivesBeforeCompletedDrives() = runTest {
      val drives = repository.observeAll().first().getOrThrow()

      assertEquals(listOf("earlier", "later", "completed"), drives.map { it.id })
      assertEquals("p1", drives.first().personId)
      assertEquals("c1", drives.first().carId)
   }

   @Test fun failedDeleteKeepsObservedDrive() = runTest {
      webservice.deleteResponse = Response.error(500, "server error".toResponseBody())
      val observed = mutableListOf<List<String>>()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
         repository.observeAll().collect { observed += it.getOrThrow().map { drive -> drive.id } }
      }

      val drive = repository.findById("earlier").getOrThrow()!!
      assertTrue(repository.remove(drive).isFailure)
      assertEquals("earlier", webservice.deletedId)
      assertEquals(listOf("earlier", "later", "completed"), observed.last())
   }

   private fun drive(id: String, start: String, completed: Boolean = false) = TDriveDto(
      id = id,
      personId = "p1",
      carId = "c1",
      start = Instant.parse(start),
      isCompleted = completed,
   )
}
