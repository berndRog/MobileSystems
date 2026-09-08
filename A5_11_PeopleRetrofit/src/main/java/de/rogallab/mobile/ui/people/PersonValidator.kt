package de.rogallab.mobile.ui.people

import android.content.Context
import android.util.Patterns
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput

/**
 * Provides reusable validation functions for person input fields.
 *
 * Every function returns null for a valid value and a localized error message
 * for an invalid value. The same validation functions can therefore be used
 * by the UI during input and by the ViewModel before saving a person.
 *
 * @param context Android context used to access localized resources.
 */
class PersonValidator(
   context: Context,
) {
   // Keep only the application context to avoid retaining an Activity
   // or another short-lived UI context.
   private val _context = context.applicationContext

   // Minimum and maximum character counts for person names.
   private val _charMin: Int by lazy {
      _context.resources.getInteger(R.integer.char_min)
   }

   private val _charMax: Int by lazy {
      _context.resources.getInteger(R.integer.char_max)
   }

   // Localized validation messages for the first name.
   private val _firstNameTooShort: String by lazy {
      _context.getString(R.string.error_firstname_too_short)
   }

   private val _firstNameTooLong: String by lazy {
      _context.getString(R.string.error_firstname_too_long)
   }

   // Localized validation messages for the last name.
   private val _lastNameTooShort: String by lazy {
      _context.getString(R.string.error_lastname_too_short)
   }

   private val _lastNameTooLong: String by lazy {
      _context.getString(R.string.error_lastname_too_long)
   }

   private val _emailInvalid: String by lazy {
      _context.getString(R.string.error_email)
   }

   private val _phoneInvalid: String by lazy {
      _context.getString(R.string.error_phone)
   }

   // Validates the complete person in the same order used by the save action.
   // The first validation error is returned; null means that all fields are valid.
   fun validatePerson(
      person: Person,
   ): String? =
      validateFirstName(person.firstName)
         ?: validateLastName(person.lastName)
         ?: validateEmail(person.email.orEmpty())
         ?: validatePhone(person.phone.orEmpty())

   // Validates the first name against the configured minimum and maximum length.
   fun validateFirstName(
      firstName: String,
   ): String? {

      // Ignore leading and trailing whitespace during validation.
      val value = firstName.trim()

      return when {
         value.length < _charMin -> _firstNameTooShort
         value.length > _charMax -> _firstNameTooLong
         else -> null
      }
   }

   // Validates the last name against the configured minimum and maximum length.
   fun validateLastName(
      lastName: String,
   ): String? {

      // Ignore leading and trailing whitespace during validation.
      val value = lastName.trim()

      return when {
         value.length < _charMin -> _lastNameTooShort
         value.length > _charMax -> _lastNameTooLong
         else -> null
      }
   }

   // Empty email addresses are allowed because the domain property is nullable.
   // Validation is intentionally independent of the input filter used by the UI.
   fun validateEmail(
      email: String,
   ): String? {

      // Remove leading and trailing whitespace before validation.
      val value = email.trim()

      // An empty value is valid because the email address is optional.
      if (value.isBlank())
         return null

      // validate the sanitized email address ä,ö,ü-> ae,oe,ue etc.
      val sanitizedEmail = sanitizeEmailInput(email)

      // Use Android's predefined email pattern to validate the complete address.
      return if (Patterns.EMAIL_ADDRESS.matcher(sanitizedEmail).matches())
         null
      else
         _emailInvalid
   }

   // Empty phone numbers are allowed because the domain property is nullable.
   // Validation is intentionally independent of the input filter used by the UI.
   fun validatePhone(
      phone: String,
   ): String? {

      // Remove leading and trailing whitespace before validation.
      val value = phone.trim()

      // An empty value is valid because the phone number is optional.
      if (value.isBlank())
         return null

      // Check whether every character belongs to the supported
      // set of phone number characters.
      val hasAllowedCharacters = value.all { character ->
         character.isDigit() ||
            character == '+' ||
            character == ' ' ||
            character == '-' ||
            character == '/' ||
            character == '.' ||
            character == '(' ||
            character == ')'
      }

      // A plus sign is optional, but if present it may occur only once
      // and must be the first character.
      val plusCount = value.count { character ->
         character == '+'
      }

      val hasValidPlus =
         plusCount == 0 ||
            (plusCount == 1 && value.startsWith('+'))

      // Formatting characters do not count towards the actual
      // length of the phone number.
      val digitCount = value.count(Char::isDigit)

      // Return the corresponding validation message or null when valid.
      return when {
         !hasAllowedCharacters -> _phoneInvalid
         !hasValidPlus -> _phoneInvalid
         digitCount !in PHONE_DIGITS_MIN..PHONE_DIGITS_MAX -> _phoneInvalid
         else -> null
      }
   }

   private companion object {

      // Defines a pragmatic range for the number of digits in a phone number.
      const val PHONE_DIGITS_MIN = 6
      const val PHONE_DIGITS_MAX = 15
   }
}

/*
 * Didaktik / Lernziele:
 *
 * - PersonValidator bündelt alle Validierungsregeln für eine Person an einer
 *   zentralen Stelle. Dadurch können dieselben Regeln sowohl in der UI als
 *   auch im ViewModel vor dem Speichern verwendet werden.
 *
 * - Der Validator erhält einen Context nur zum Zugriff auf Android-Ressourcen.
 *   Intern wird der applicationContext gespeichert, damit keine Activity oder
 *   andere kurzlebige UI-Komponente festgehalten wird.
 *
 * - Grenzwerte und Fehlermeldungen werden aus Ressourcen gelesen. Dadurch
 *   bleiben konfigurierbare Werte und lokalisierte Texte außerhalb des Codes.
 *
 * - Die Validatoren verwenden ein einheitliches Rückgabekonzept:
 *   null   -> Eingabe ist gültig
 *   String -> Eingabe ist ungültig; der String enthält die Fehlermeldung.
 *
 * - validatePerson() kombiniert die einzelnen Prüfungen mit dem Elvis-Operator
 *   ?: und liefert die erste gefundene Fehlermeldung zurück.
 *
 * - Sanitizing und Validierung bleiben bewusst getrennte Aufgaben.
 *   sanitizeEmailInput() und sanitizePhoneInput() verändern bzw. filtern
 *   Benutzereingaben bereits in der UI.
 *
 * - validateEmail() und validatePhone() prüfen dagegen den tatsächlich
 *   vorhandenen Wert erneut und verlassen sich nicht darauf, dass zuvor
 *   ein Sanitizer verwendet wurde. Dadurch bleiben die Validatoren auch für
 *   Werte aus Datenbanken, Tests, Importen oder anderen Eingabequellen nutzbar.
 *
 * - Optionale Werte wie E-Mail-Adresse und Telefonnummer dürfen leer sein.
 *   Ein leerer String wird deshalb als gültig behandelt.
 *
 * - Bei Telefonnummern werden Formatierungszeichen zugelassen, für die
 *   Längenprüfung zählen jedoch ausschließlich die enthaltenen Ziffern.
 *
 * - Die Klasse zeigt außerdem, wann eine eigene Validator-Komponente sinnvoll
 *   ist: wenn dieselbe Validierungslogik einschließlich lokalisierter
 *   Fehlermeldungen von mehreren Schichten wiederverwendet werden soll.
 */

