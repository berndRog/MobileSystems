package de.rogallab.mobile.ui.people.input_detail

import android.content.Context
import android.util.Patterns
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person

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

   fun validatePerson(person: Person): String? =
      validateFirstName(person.firstName)
         ?: validateLastName(person.lastName)
         ?: validateEmail(person.email.orEmpty())
         ?: validatePhone(person.phone.orEmpty())

   fun validateFirstName(firstName: String): String? {
      val normalizedFirstName = firstName.trim()
      return when {
         normalizedFirstName.length < _charMin ->
            _context.getString(R.string.error_first_name_too_short)
         normalizedFirstName.length > _charMax ->
            _context.getString(R.string.error_first_name_too_long)
         else -> null
      }
   }

   fun validateLastName(lastName: String): String? {
      val normalizedLastName = lastName.trim()
      return when {
         normalizedLastName.length < _charMin ->
            _context.getString(R.string.error_last_name_too_short)
         normalizedLastName.length > _charMax ->
            _context.getString(R.string.error_last_name_too_long)
         else -> null
      }
   }

   fun validateEmail(email: String): String? {
      val normalizedEmail = email.trim()
      if (normalizedEmail.isBlank()) return null
      return if (Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
         null
      }
      else {
         _context.getString(R.string.error_email)
      }
   }

   fun validatePhone(phone: String): String? {
      val normalizedPhone = phone.trim()
      if (normalizedPhone.isBlank()) return null

      val hasAllowedCharacters = PHONE_CHARACTERS.matches(normalizedPhone)
      val plusCount = normalizedPhone.count { character -> character == '+' }
      val hasValidPlus = plusCount == 0 ||
         (plusCount == 1 && normalizedPhone.startsWith('+'))
      val digitCount = normalizedPhone.count { character -> character.isDigit() }

      return when {
         !hasAllowedCharacters -> _context.getString(R.string.error_phone)
         !hasValidPlus -> _context.getString(R.string.error_phone)
         digitCount !in PHONE_DIGITS_MIN..PHONE_DIGITS_MAX ->
            _context.getString(R.string.error_phone)
         else -> null
      }
   }

   private companion object {
      const val PHONE_DIGITS_MIN = 6
      const val PHONE_DIGITS_MAX = 15
      val PHONE_CHARACTERS = Regex(pattern = """^[0-9+()\s./-]+$""")
   }
}

fun sanitizePhoneInput(input: String): String =
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
