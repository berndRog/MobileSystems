package de.rogallab.mobile.shared.domain.utilities

import android.app.Application
import android.net.Uri
import de.rogallab.mobile.shared.ui.common.toImageModel as uiToImageModel
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class ToImageModelTest {

   @Test
   fun domain_contentUriString_returnsUri() {
      val value = "content://images/person/1"

      val model = value.toImageModel()

      assertTrue(model is Uri)
      assertEquals(value, model.toString())
   }

   @Test
   fun domain_fileUriString_returnsUri() {
      val value = "file:///tmp/person.jpg"

      val model = value.toImageModel()

      assertTrue(model is Uri)
      assertEquals(value, model.toString())
   }

   @Test
   fun domain_plainPath_returnsFile() {
      val value = "/data/user/0/app/files/images/person.jpg"

      val model = value.toImageModel()

      assertTrue(model is File)
      assertEquals(value, (model as File).path)
   }

   @Test
   fun ui_contentUriString_returnsUri() {
      val value = "content://images/person/1"

      val model = value.uiToImageModel()

      assertTrue(model is Uri)
      assertEquals(value, model.toString())
   }

   @Test
   fun ui_fileUriString_returnsUri() {
      val value = "file:///tmp/person.jpg"

      val model = value.uiToImageModel()

      assertTrue(model is Uri)
      assertEquals(value, model.toString())
   }

   @Test
   fun ui_plainPath_returnsFile() {
      val value = "/data/user/0/app/files/images/person.jpg"

      val model = value.uiToImageModel()

      assertTrue(model is File)
      assertEquals(value, (model as File).path)
   }
}
