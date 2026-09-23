package de.rogallab.mobile.testing

import de.rogallab.mobile.shared.ui.images.IImageEdit

class FakeImageEdit : IImageEdit {
   var commitCount = 0
   var discardCount = 0
   private var images: List<String> = emptyList()

   override fun start(imagePaths: List<String>) {
      images = imagePaths
   }

   override suspend fun replace(imagePaths: List<String>): List<String> {
      images = imagePaths
      return images
   }

   override suspend fun add(imagePaths: List<String>): List<String> =
      replace(images + imagePaths)

   override suspend fun remove(imagePath: String): List<String> =
      replace(images.filterNot { it == imagePath })

   override suspend fun commit() {
      commitCount++
   }

   override suspend fun discard() {
      discardCount++
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Der Fake macht die Interaktion mit IImageEdit beobachtbar, ohne echte
 *   Dateien anzulegen oder Android-Infrastruktur zu benötigen.
 *
 * - commitCount und discardCount zeigen, ob ein Use Case die Bild-Session im
 *   jeweiligen Erfolgs- oder Fehlerpfad korrekt abschließt.
 *
 * Lernziele:
 *
 * - Abhängigkeiten in Unit-Tests durch kontrollierbare Fakes ersetzen.
 * - Seiteneffekte über beobachtbare Testzustände verifizieren.
 */
