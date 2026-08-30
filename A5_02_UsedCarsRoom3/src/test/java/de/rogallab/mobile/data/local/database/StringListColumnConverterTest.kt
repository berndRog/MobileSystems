package de.rogallab.mobile.data.local.database

import org.junit.Assert.assertEquals
import org.junit.Test

class StringListColumnConverterTest {

   @Test
   fun imagePaths_roundTrip_preservesOrderAndValues() {
      val imagePaths = listOf(
         "/private/images/front.jpg",
         "/private/images/rear.png",
      )

      val json = StringListColumnConverter.toJson(imagePaths)
      val restoredImagePaths = StringListColumnConverter.fromJson(json)

      assertEquals(imagePaths, restoredImagePaths)
   }

   @Test
   fun blankJson_returnsEmptyList() {
      val restoredImagePaths = StringListColumnConverter.fromJson("")

      assertEquals(emptyList<String>(), restoredImagePaths)
   }
}
