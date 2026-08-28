package de.rogallab.mobile.ui.people

import de.rogallab.mobile.domain.entities.Person

// Maps an empty text-field value back to the nullable domain representation. */
internal fun String.toNullableInput(): String? =
   trim().takeUnless(String::isEmpty)

// Normalizes all user-editable values before the final save validation. */
internal fun Person.normalized(): Person =
   copy(
      firstName = firstName.trim(),
      lastName = lastName.trim(),
      email = email?.trim()?.takeUnless(String::isEmpty),
      phone = phone?.trim()?.takeUnless(String::isEmpty),
      imagePath = imagePath?.takeUnless(String::isBlank),
   )
