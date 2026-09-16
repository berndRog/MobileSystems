package de.rogallab.mobile.data.repositories

import android.app.Application
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.testing.FakeCarWebservice
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class CarRepositoryImageTest {
   @get:Rule val temporaryFolder = TemporaryFolder()
   private val webservice = FakeCarWebservice()
   private val repository = CarRepository(webservice)

   @Test
   fun createWithLocalImage_sendsJsonThenUploadsFile() = runTest {
      val image = temporaryFolder.newFile("car.jpg").apply { writeBytes(byteArrayOf(1)) }

      val result = repository.create(car(listOf(image.absolutePath)))

      assertTrue(result.isSuccess)
      assertEquals(listOf("create", "uploadImage:c1"), webservice.calls)
      assertEquals(emptyList<String>(), webservice.createdDto?.imageUrls)
   }

   @Test
   fun updateWithExistingServerImage_keepsItWithoutImageRequest() = runTest {
      val result = repository.update(car(listOf(FakeCarWebservice.IMAGE_URL)))

      assertTrue(result.isSuccess)
      assertEquals(listOf("update:c1"), webservice.calls)
      assertEquals(emptyList<String>(), webservice.updatedDto?.imageUrls)
   }

   @Test
   fun updateWithReplacement_deletesOldImageThenUploadsLocalFile() = runTest {
      val image = temporaryFolder.newFile("replacement.jpg").apply { writeBytes(byteArrayOf(2)) }

      val result = repository.update(car(listOf(image.absolutePath)))

      assertTrue(result.isSuccess)
      assertEquals(
         listOf("update:c1", "deleteImage:c1:original.jpg", "uploadImage:c1"),
         webservice.calls,
      )
   }

   private fun car(imagePaths: List<String>) = Car(
      id = "c1",
      manufacturer = "Fiat",
      model = "500",
      price = 18_900,
      imagePaths = imagePaths,
      personId = "p1",
   )
}
