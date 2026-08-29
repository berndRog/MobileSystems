package de.rogallab.mobile.ui.people.input_detail

import android.content.Context
import android.util.Patterns
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person

/**
 * Provides reusable validation functions for person input fields.
 *
 * Every function returns null for a valid value and a localized error message
 * for an invalid value. The same functions can therefore be used directly by
 * InputString and by a ViewModel before a save operation.
 */
class PersonValidator(
   context: Context,
) {
   private val _context = context.applicationContext

   private val _charMin: Int by lazy {
      _context.resources.getInteger(R.integer.person_name_char_min)
   }

   private val _charMax: Int by lazy {
      _context.resources.getInteger(R.integer.person_name_char_max)
   }

   private val _firstNameTooShort: String by lazy {
      _context.getString(R.string.error_first_name_too_short)
   }
   private val _firstNameTooLong: String by lazy {
      _context.getString(R.string.error_first_name_too_long)
   }

   private val _lastNameTooShort: String by lazy {
      _context.getString(R.string.error_last_name_too_short)
   }
   private val _lastNameTooLong: String by lazy {
      _context.getString(R.string.error_last_name_too_long)
   }

   private val _emailInvalid: String by lazy {
      _context.getString(R.string.error_email)
   }
   private val _phoneInvalid: String by lazy {
      _context.getString(R.string.error_phone)
   }

   /**
    * Validates the complete form in the same order used by the save action.
    * The first error message is returned; null means that all fields are valid.
    */
   fun validatePerson(
      person: Person,
   ): String? =
      validateFirstName(person.firstName)
         ?: validateLastName(person.lastName)
         ?: validateEmail(person.email.orEmpty())
         ?: validatePhone(person.phone.orEmpty())

   fun validateFirstName(
      firstName: String,
   ): String? {
      val value = firstName.trim()

      return when {
         value.length < _charMin -> _firstNameTooShort
         value.length > _charMax -> _firstNameTooLong
         else -> null
      }
   }

   fun validateLastName(
      lastName: String,
   ): String? {
      val value = lastName.trim()

      return when {
         value.length < _charMin -> _lastNameTooShort
         value.length > _charMax -> _lastNameTooLong
         else -> null
      }
   }

   /** Empty email addresses are allowed because the domain property is nullable. */
   fun validateEmail(
      email: String,
   ): String? {
      val value = email.trim()
      if (value.isBlank()) return null

      if (Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
         return null
      }
      else {
         return _emailInvalid
      }
   }

   /**
    * Empty phone numbers are allowed because the domain property is nullable.
    * The check is intentionally pragmatic and does not model every national
    * phone-number format.
    */
   fun validatePhone(
      phone: String,
   ): String? {
      val value = phone.trim()
      if (value.isBlank()) return null

      val hasAllowedCharacters = PHONE_CHARACTERS.matches(value)
      val plusCount = value.count { character -> character == '+' }
      val hasValidPlus = plusCount == 0 ||
         (plusCount == 1 && value.startsWith('+'))
      val digitCount = value.count(Char::isDigit)

      return when {
         !hasAllowedCharacters -> _phoneInvalid
         !hasValidPlus -> _phoneInvalid
         digitCount !in PHONE_DIGITS_MIN..PHONE_DIGITS_MAX -> _phoneInvalid
         else -> null
      }
   }

   private companion object {
      const val PHONE_DIGITS_MIN = 6
      const val PHONE_DIGITS_MAX = 15
      val PHONE_CHARACTERS = Regex(pattern = """^[0-9+()\s./-]+$""")
   }
}

/**
 * Filters phone input without forcing a storage format while the user types.
 * A plus sign is accepted only as the first character.
 */
fun sanitizePhoneInput(
   input: String,
): String =
   buildString {
      input.forEach { character ->
         when {
            character.isDigit() -> append(character)
            character == '+' && isEmpty() -> append(character)
            character == ' ' -> append(character)
            character == '-' -> append(character)
            character == '/' -> append(character)
            character == '.' -> append(character)
            character == '(' -> append(character)
            character == ')' -> append(character)
         }
      }
   }.take(30)
