package de.rogallab.mobile.ui.people

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.domain.entities.Person
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class PersonValidatorTest {

   private val validator =
      PersonValidator(ApplicationProvider.getApplicationContext())

   @Test
   fun validatePerson_validPersonReturnsNull() {
      val person = Person(
         firstName = "Ada",
         lastName = "Lovelace",
         email = "ada@example.org",
         phone = "+49 511 123456",
      )

      assertNull(validator.validatePerson(person))
   }

   @Test
   fun validateFirstName_tooShortReturnsError() {
      assertNotNull(validator.validateFirstName("A"))
   }

   @Test
   fun validateEmail_invalidAddressReturnsError() {
      assertNotNull(validator.validateEmail("not-an-email"))
   }
}
