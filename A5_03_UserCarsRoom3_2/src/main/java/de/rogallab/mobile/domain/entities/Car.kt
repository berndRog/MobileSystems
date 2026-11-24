package de.rogallab.mobile.domain.entities

import de.rogallab.mobile.domain.utilities.newUuid

data class Car(
   val manufacturer: String = "",
   val model: String = "",
   val price: Int? = null,
   val imagePaths: List<String> = emptyList(),
   val id: String = newUuid(),
   val personId: String? = null
) {
   val displayName: String
      get() = "$manufacturer $model".trim()

   val primaryImagePath: String?
      get() = imagePaths.firstOrNull { imagePath ->
         imagePath.isNotBlank()
      }
}
