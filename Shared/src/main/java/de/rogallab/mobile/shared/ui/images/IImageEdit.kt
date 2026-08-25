package de.rogallab.mobile.shared.ui.images

interface IImageEdit {

   // Starts an edit session with the images currently stored by the entity.
   fun start(imagePaths: List<String>)
   fun start(imagePath: String?)

   // Replaces the current selection.
   suspend fun replace(imagePaths: List<String>): List<String>
   suspend fun replace(imagePath: String?): String?

   // Adds images to the current selection.
   suspend fun add(imagePaths: List<String>): List<String>

   // Removes one image from the current selection.
   suspend fun remove(imagePath: String): List<String>

   // Completes a successful Save.
   suspend fun commit()

   // Discards the edit session.
   suspend fun discard()

}