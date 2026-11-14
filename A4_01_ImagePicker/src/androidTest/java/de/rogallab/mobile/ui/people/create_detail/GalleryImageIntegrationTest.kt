package de.rogallab.mobile.ui.people.create_detail

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.data.local.io.ImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.ImageEdit
import de.rogallab.mobile.ui.people.PersonValidator
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GalleryImageIntegrationTest {
   private val context = ApplicationProvider.getApplicationContext<Context>()

   @Test fun galleryUriIsCopiedToReadablePrivateImageAndPersonState() = runBlocking {
      val storage = ImageFileStorage(context, "gallery-test-${UUID.randomUUID()}")
      val source = storage.createCameraImageFile().getOrThrow()
      var copiedPath: String? = null
      try {
         // A real JPEG behind a controlled content Uri; no gallery app is launched.
         val bitmap = Bitmap.createBitmap(3, 2, Bitmap.Config.ARGB_8888)
         bitmap.eraseColor(android.graphics.Color.BLUE)
         File(source.imagePath).outputStream().use { output ->
            assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 100, output))
         }
         bitmap.recycle()

         val repository = EmptyRepository()
         val imageEdit = ImageEdit(storage)
         val viewModel = PersonViewModel(
            personId = null,
            _repository = repository,
            _stringProvider = StringProvider(context),
            _validator = PersonValidator(context),
            _imageFileStorage = storage,
            _imageEdit = imageEdit,
            _personUcCreate = PersonUcCreate(repository, imageEdit),
            _personUcUpdate = PersonUcUpdate(repository, imageEdit),
            _effectDelegate = EffectDelegate(),
            _timing = AppTiming(delayRead = 10L, delayWrite = 10L, animationDuration = 10),
         )

         viewModel.onIntent(PersonIntent.GalleryImageSelected(source.contentUri))
         copiedPath = withTimeout(5_000) {
            viewModel.stateFlow.first { it.person.imagePath != null }.person.imagePath
         }

         val copied = File(requireNotNull(copiedPath))
         assertNotEquals(File(source.imagePath).canonicalPath, copied.canonicalPath)
         assertTrue(copied.isFile)
         assertTrue(copied.length() > 0)
         val decoded = BitmapFactory.decodeFile(copied.absolutePath)
         assertEquals(3, decoded.width)
         assertEquals(2, decoded.height)
         decoded.recycle()
         assertEquals(copied.absolutePath, viewModel.stateFlow.value.person.imagePath)
      } finally {
         storage.deleteImageFromAppStorage(copiedPath)
         storage.deleteImageFromAppStorage(source.imagePath)
      }
   }

   private class EmptyRepository : IPersonRepository {
      override fun observeAll(): Flow<Result<List<Person>>> = flowOf(Result.success(emptyList()))
      override suspend fun findById(id: String): Result<Person?> = Result.success(null)
      override suspend fun create(person: Person): Result<Unit> = Result.success(Unit)
      override suspend fun update(person: Person): Result<Unit> = Result.success(Unit)
      override suspend fun remove(person: Person): Result<Unit> = Result.success(Unit)
   }
}
