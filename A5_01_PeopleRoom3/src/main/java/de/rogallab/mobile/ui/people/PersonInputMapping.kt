package de.rogallab.mobile.ui.people

import de.rogallab.mobile.ui.people.create_detail.PersonForm

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
fun PersonForm.normalized(): PersonForm = copy(
      firstName = firstName.trim(),
      lastName = lastName.trim(),
      email = sanitizeEmailInput(email.trim()),
      phone = sanitizePhoneInput(phone.trim()),
   )

// Domain → Formular: null wird zu "", damit TextFields einen String haben.
fun Person.toPersonForm(): PersonForm = PersonForm(
   id = id,
   firstName = firstName,
   lastName = lastName,
   email = email.orEmpty(),
   phone = phone.orEmpty(),
   imagePath = imagePath,
)

// Formular → Domain: erst nach erfolgreicher Validierung aufrufen.
// "" wird zu null, die Person ist danach gültig.
fun PersonForm.toPerson(): Person = Person(
   id = id,
   firstName = firstName,
   lastName = lastName,
   email = email.toNullableInput(),
   phone = phone.toNullableInput(),
   imagePath = imagePath,
)
