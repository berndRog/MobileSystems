package de.rogallab.mobile.ui.people

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.domain.utilities.sanitizeEmailInput
import de.rogallab.mobile.shared.domain.utilities.sanitizePhoneInput

// Maps an empty text-field value to the nullable domain representation.
// fun String.toNullableInput(): String? {
//    val value = trim()
//    return if (value.isEmpty()) null else value
// }
// Kotlin idiomatic
fun String.toNullableInput(): String? =
   trim().takeUnless(String::isEmpty) // String::isEmpty gives null

// Normalizes all user-editable values before the final save validation.
internal fun Person.normalized(): Person =
   copy(
      firstName = firstName.trim(),
      lastName = lastName.trim(),
      email = email?.toNullableInput()?.let(::sanitizeEmailInput)?.toNullableInput(),
      phone = phone?.toNullableInput()?.let(::sanitizePhoneInput)?.toNullableInput(),
      imagePath = imagePath?.toNullableInput()
   )
