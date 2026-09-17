package de.rogallab.mobile.data.repositories

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.shared.data.network.ServerUnreachableException
import de.rogallab.mobile.testing.FakePersonWebservice
import java.io.File
import java.net.UnknownHostException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class PersonRepositoryImageTest {

   @get:Rule
   val temporaryFolder = TemporaryFolder()

   private val webservice = FakePersonWebservice()
   private val context =
      ApplicationProvider.getApplicationContext<android.content.Context>()
   private val repository = PersonRepository(
      _webservice = webservice,
      _networkExceptionMapper = NetworkExceptionMapper(context),
   )

   @Test
   fun createWithLocalImage_sendsJsonThenUploadsFile() = runTest {
      val imageFile = imageFile()
      val result = repository.create(person(imageFile.absolutePath))

      assertTrue(result.isSuccess)
      assertEquals(listOf("create", "uploadImage:p1"), webservice.calls)
      assertNull(webservice.createdDto?.imageUrl)
      assertTrue(
         webservice.uploadedPart
            ?.headers
            ?.get("Content-Disposition")
            ?.contains("name=\"file\"") == true
      )
   }

   @Test
   fun createWhenUploadFails_rollsBackCreatedPerson() = runTest {
      webservice.uploadFailure = IllegalStateException("upload failed")
      val result = repository.create(person(imageFile().absolutePath))

      assertTrue(result.isFailure)
      assertEquals(
         listOf("create", "uploadImage:p1", "delete:p1"),
         webservice.calls,
      )
   }

   @Test
   fun createWhenServerIsUnreachable_rollsBackAndMapsFailure() = runTest {
      webservice.uploadFailure = UnknownHostException("people api")
      val result = repository.create(person(imageFile().absolutePath))

      assertTrue(result.exceptionOrNull() is ServerUnreachableException)
      assertEquals(
         listOf("create", "uploadImage:p1", "delete:p1"),
         webservice.calls,
      )
   }

   @Test
   fun updateWithServerUrl_keepsImageWithoutMultipartRequest() = runTest {
      val result = repository.update(person(FakePersonWebservice.IMAGE_URL))

      assertTrue(result.isSuccess)
      assertEquals(listOf("update:p1"), webservice.calls)
      assertNull(webservice.updatedDto?.imageUrl)
   }

   @Test
   fun updateWithLocalImage_sendsJsonThenUploadsFile() = runTest {
      val result = repository.update(person(imageFile().absolutePath))

      assertTrue(result.isSuccess)
      assertEquals(listOf("update:p1", "uploadImage:p1"), webservice.calls)
      assertNull(webservice.updatedDto?.imageUrl)
   }

   @Test
   fun updateWithoutImage_sendsJsonThenDeletesServerImage() = runTest {
      val result = repository.update(person(imagePath = null))

      assertTrue(result.isSuccess)
      assertEquals(listOf("update:p1", "deleteImage:p1"), webservice.calls)
      assertNull(webservice.updatedDto?.imageUrl)
   }

   private fun imageFile(): File =
      temporaryFolder.newFile("person.jpg").apply {
         writeBytes(byteArrayOf(1, 2, 3))
      }

   private fun person(imagePath: String?) = Person(
      firstName = "Ada",
      lastName = "Lovelace",
      imagePath = imagePath,
      id = "p1",
   )
}
