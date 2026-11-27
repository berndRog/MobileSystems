package de.rogallab.mobile.data.readmodels

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.data.mapping.toPerson
import de.rogallab.mobile.data.mapping.toPersonListItem
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.testing.FakePersonWebservice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class PersonReadModelTest {
   private val web = FakePersonWebservice()
   private val repository = PersonRepository(
      web, NetworkExceptionMapper(ApplicationProvider.getApplicationContext())
   )

   @Test fun dataMapping_preservesProjectionAndServerImageReference() {
      val person = FakePersonWebservice.personDto(imageUrl = FakePersonWebservice.IMAGE_URL).toPerson()
      val item = person.toPersonListItem()
      assertEquals(person.id, item.id)
      assertEquals(person.fullName, item.fullName)
      assertEquals(person.email, item.email)
      assertEquals(person.phone, item.phone)
      assertEquals(FakePersonWebservice.IMAGE_URL, item.imagePath)
   }

   @Test fun successfulCreateUpdateDelete_updateTheSameObservation() = runTest {
      val dispatcher = UnconfinedTestDispatcher(testScheduler)
      val model = PersonReadModel(repository, dispatcher)
      val observed = mutableListOf<List<de.rogallab.mobile.domain.readmodels.PersonListItem>>()
      backgroundScope.launch(dispatcher) {
         model.observeListItems().collect { observed += it.getOrThrow() }
      }
      assertTrue(observed.last().isEmpty())
      val person = FakePersonWebservice.personDto().toPerson()
      assertTrue(repository.create(person).isSuccess)
      assertEquals(listOf(person.toPersonListItem()), observed.last())

      web.updateResult = FakePersonWebservice.personDto(imageUrl = FakePersonWebservice.IMAGE_URL)
         .copy(firstName = "Grace", lastName = "Hopper")
      assertTrue(repository.update(person.copy(imagePath = FakePersonWebservice.IMAGE_URL)).isSuccess)
      assertEquals("Grace Hopper", observed.last().single().fullName)
      assertEquals(FakePersonWebservice.IMAGE_URL, observed.last().single().imagePath)

      assertTrue(repository.remove(person).isSuccess)
      assertTrue(observed.last().isEmpty())
      assertEquals(1, web.calls.count { it == "getAll" })
   }

   @Test fun failedWrites_leaveProjectionUnchanged() = runTest {
      web.people += FakePersonWebservice.personDto()
      val dispatcher = UnconfinedTestDispatcher(testScheduler)
      val observed = mutableListOf<List<de.rogallab.mobile.domain.readmodels.PersonListItem>>()
      backgroundScope.launch(dispatcher) {
         PersonReadModel(repository, dispatcher).observeListItems().collect { observed += it.getOrThrow() }
      }
      val before = observed.last()
      web.deleteResponse = Response.error(500, "server error".toResponseBody())
      assertTrue(repository.remove(web.people.single().toPerson()).isFailure)
      assertEquals(before, observed.last())
   }

   @Test fun httpAndNetworkFailures_areRetryable_andCancellationPropagates() = runTest {
      val dispatcher = UnconfinedTestDispatcher(testScheduler)
      val model = PersonReadModel(repository, dispatcher)
      for (failure in listOf(
         HttpException(Response.error<Unit>(503, "unavailable".toResponseBody())),
         java.io.IOException("offline")
      )) {
         web.getAllFailure = failure
         assertTrue(model.observeListItems().first().isFailure)
         web.getAllFailure = null
         web.people = mutableListOf(FakePersonWebservice.personDto())
         assertEquals("Ada Lovelace", model.observeListItems().first().getOrThrow().single().fullName)
      }
      val cancellation = CancellationException("cancelled")
      web.getAllFailure = cancellation
      try {
         model.observeListItems().first()
         fail("Cancellation must propagate")
      } catch (actual: CancellationException) {
         // flowOn may recover the stack trace by copying the exception.
         assertEquals(cancellation.message, actual.message)
      }
   }

   @Test fun cancellingCollector_stopsFurtherEmissions() = runTest {
      val dispatcher = UnconfinedTestDispatcher(testScheduler)
      var emissions = 0
      val job = backgroundScope.launch(dispatcher) {
         PersonReadModel(repository, dispatcher).observeListItems().collect { emissions++ }
      }
      assertEquals(1, emissions)
      job.cancel()
      job.join()
      repository.create(FakePersonWebservice.personDto().toPerson()).getOrThrow()
      assertEquals(1, emissions)
   }
}
