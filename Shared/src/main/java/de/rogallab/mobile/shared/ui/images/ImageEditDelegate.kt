package de.rogallab.mobile.shared.ui.images

import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog

/**
 * Manages image files that belong to one edit session.
 *
 * The delegate is independent of Person, Car or another feature. It only
 * compares the images that existed when editing started with the images that
 * are currently selected.
 */
class ImageEditDelegate(
   private val _imageFileStorage: IImageFileStorage,
): IImageEdit {
   private var _originalImages: List<String> = emptyList()
   private var _images: List<String> = emptyList()

   // Starts an edit session with the images currently stored by the entity.
   override fun start(imagePaths: List<String>) {
      val images = normalized(imagePaths)
      _originalImages = images
      _images = images
   }

   // Replaces the current selection. Images created during this edit session
   // that are no longer selected can be deleted immediately.
   override suspend fun replace(imagePaths: List<String>): List<String> {
      val images = normalized(imagePaths)
      val obsoleteImages = _images.filter { imagePath ->
         imagePath !in _originalImages && imagePath !in images
      }

      _images = images
      deleteImages(obsoleteImages, "delete replacement image failed")
      return _images
   }

   // Adds images to the current selection. This is useful for entities such as
   // cars that may own more than one image.
   override suspend fun add(imagePaths: List<String>): List<String> =
      replace(_images + imagePaths)

   // Removes one image from the current selection without deleting a persisted
   // original before Save has succeeded.
   override suspend fun remove(imagePath: String): List<String> =
      replace(_images.filterNot { it == imagePath })

   // Completes a successful Save. Persisted originals that are no longer used
   // can now be deleted safely.
   override suspend fun commit() {
      val obsoleteOriginals = _originalImages.filter { it !in _images }
      deleteImages(obsoleteOriginals, "delete original image failed")
      _originalImages = _images
   }

   // Discards the edit session. Only images created during this session are
   // removed; persisted originals remain untouched.
   override suspend fun discard() {
      val unsavedImages = _images.filter { it !in _originalImages }
      deleteImages(unsavedImages, "delete unsaved image failed")
      _images = _originalImages
   }

   private fun normalized(imagePaths: List<String>): List<String> =
      imagePaths.mapNotNull { it.takeUnless(String::isBlank) }.distinct()

   private suspend fun deleteImages(imagePaths: List<String>, message: String) {
      imagePaths.forEach { imagePath ->
         _imageFileStorage.deleteImageFromAppStorage(imagePath)
            .onFailure { throwable ->
               Alog.e(TAG, "$message: ${throwable.message}")
            }
      }
   }

   companion object {
      private const val TAG = "<-ImageEditDelegate"
   }
}

/*
 * Didaktik und Lernziele
 *
 * - ImageEditDelegate kapselt die technische Lebensdauer von Bilddateien und
 *   kennt keine fachliche Entität wie Person oder Car.
 *
 * - start(...) übernimmt den persistenten Ausgangszustand einer Bearbeitung.
 *   replace(...), add(...) und remove(...) ändern nur die aktuelle Auswahl.
 *
 * - Ein persistiertes Originalbild wird erst nach erfolgreichem Save durch
 *   commit() gelöscht. cancel() entfernt dagegen nur neu erzeugte, noch nicht
 *   persistierte Bilder.
 *
 * - Der Delegate besitzt bewusst keinen eigenen StateFlow. Der Feature-State
 *   bleibt weiterhin der einzige StateFlow, den der Screen beobachtet.
 *
 * Lernziele:
 *
 * - Gemeinsame Logik durch Delegation statt durch ViewModel-Vererbung nutzen.
 * - Fachlichen UI-State von technischer Dateiverwaltung trennen.
 * - Dieselbe Bildlogik für Einzel- und Mehrfachbilder wiederverwenden.
 */
