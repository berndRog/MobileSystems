package de.rogallab.mobile.shared.domain.utilities

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.shared.R
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
class StringProviderTest {

   private val context =
      ApplicationProvider.getApplicationContext<Application>()

   private val provider = StringProvider(context)

   @Test
   fun getString_returnsStringResource() {
      assertEquals(
         context.getString(R.string.error_image_save),
         provider.getString(R.string.error_image_save)
      )
   }

   @Test
   fun getString_withArguments_delegatesToContext() {
      assertEquals(
         context.getString(android.R.string.ok, "unused"),
         provider.getString(android.R.string.ok, "unused")
      )
   }
}
