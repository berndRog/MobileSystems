package de.rogallab.mobile.domain.entities

data class Car(
   val manufacturer: String = "",
   val model: String = "",
   val registrationYear: Int? = null,
   val mileage: Int? = null,
   val priceInEuro: Int? = null,
   val sellerId: String? = null,
   val imagePaths: List<String> = emptyList(),
   val id: String,
) {
   val displayName: String
      get() = "$manufacturer $model".trim()

   val primaryImagePath: String?
      get() = imagePaths.firstOrNull { imagePath ->
         imagePath.isNotBlank()
      }
}
