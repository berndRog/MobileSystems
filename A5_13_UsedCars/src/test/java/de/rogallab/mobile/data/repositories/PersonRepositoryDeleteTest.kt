package de.rogallab.mobile.data.repositories

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.testing.FakePersonWebservice
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class PersonRepositoryDeleteTest {
   private val webservice = FakePersonWebservice().apply {
      people += FakePersonWebservice.personDto(id = "p1")
      people += FakePersonWebservice.personDto(id = "p2")
   }
   private val repository = PersonRepository(
      _webservice = webservice,
      _networkExceptionMapper = NetworkExceptionMapper(ApplicationProvider.getApplicationContext()),
   )

   @Test fun successfulDeleteRemovesPersonFromObservedState() = runTest {
      val observed = mutableListOf<List<String>>()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
         repository.observeAll().collect { observed += it.getOrThrow().map { person -> person.id } }
      }
      assertEquals(listOf("p1", "p2"), observed.last())

      val person = repository.findById("p1").getOrThrow()!!
      assertTrue(repository.remove(person).isSuccess)

      assertEquals(listOf("delete:p1"), webservice.calls)
      assertEquals(listOf("p2"), observed.last())
   }

   @Test fun failedDeleteKeepsPersonInObservedState() = runTest {
      webservice.deleteResponse = Response.error(500, "server error".toResponseBody())
      val observed = mutableListOf<List<String>>()
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
         repository.observeAll().collect { observed += it.getOrThrow().map { person -> person.id } }
      }

      val person = repository.findById("p1").getOrThrow()!!
      assertTrue(repository.remove(person).isFailure)
      assertEquals(listOf("p2", "p1"), observed.last())
   }
}
