package de.rogallab.mobile.shared.domain.io

import android.app.Application
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class ImageIoTypesTest {

   @Test
   fun imageFileFormat_exposesExpectedExtensions() {
      assertEquals(".jpg", ImageFileFormat.Jpeg.extension)
      assertEquals(".png", ImageFileFormat.Png.extension)
      assertEquals(".webp", ImageFileFormat.Webp.extension)
   }

   @Test
   fun cameraImageFile_preservesPathAndContentUri() {
      val uri = Uri.parse("content://camera/image/1")

      val file = CameraImageFile(
         imagePath = "/images/camera.jpg",
         contentUri = uri,
      )

      assertEquals("/images/camera.jpg", file.imagePath)
      assertEquals(uri, file.contentUri)
   }
}
