package de.rogallab.mobile.domain.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImagePathNormalizationTest {

   @Test
   fun normalizedImagePath_rejectsEmptyAndResourceValues() {
      assertNull("  ".normalizedImagePath())
      assertNull(
         "android.resource://de.rogallab.mobile/drawable/sample"
            .normalizedImagePath()
      )
   }

   @Test
   fun normalizedImagePaths_preservesValidOrderAndRemovesDuplicates() {
      val imagePaths = listOf(
         "",
         "android.resource://de.rogallab.mobile/drawable/sample",
         "/data/user/0/app/files/image-a.jpg",
         "/data/user/0/app/files/image-a.jpg",
         "/data/user/0/app/files/image-b.jpg",
      )

      assertEquals(
         listOf(
            "/data/user/0/app/files/image-a.jpg",
            "/data/user/0/app/files/image-b.jpg",
         ),
         imagePaths.normalizedImagePaths(),
      )
   }
}
