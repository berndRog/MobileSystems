package de.rogallab.mobile.shared.ui.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerActionsTest {

   @Test
   fun galleryPickerActions_invokesSelectCallback() {
      var calls = 0
      val actions = GalleryPickerActions(
         selectFromGallery = { calls++ }
      )

      actions.selectFromGallery()

      assertEquals(1, calls)
   }

   @Test
   fun cameraPickerActions_exposesBusyStateAndInvokesTakePhoto() {
      var calls = 0
      val actions = CameraPickerActions(
         isBusy = true,
         takePhoto = { calls++ },
      )

      assertTrue(actions.isBusy)
      actions.takePhoto()
      assertEquals(1, calls)
   }

   @Test
   fun gallerySelectionMode_containsSingleAndMultiple() {
      val values = GallerySelectionMode.entries

      assertTrue(GallerySelectionMode.Single in values)
      assertTrue(GallerySelectionMode.Multiple in values)
      assertFalse(values.isEmpty())
   }
}
