package de.rogallab.mobile.ui.people

import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.normalizedImagePath

fun String.toNullableInput(): String? =
   trim().takeUnless { normalizedValue -> normalizedValue.isBlank() }

fun Person.normalized(): Person =
   copy(
      firstName = firstName.trim(),
      lastName = lastName.trim(),
      email = email?.trim()?.takeUnless { normalizedEmail -> normalizedEmail.isBlank() },
      phone = phone?.trim()?.takeUnless { normalizedPhone -> normalizedPhone.isBlank() },
      imagePath = imagePath.normalizedImagePath(),
   )
