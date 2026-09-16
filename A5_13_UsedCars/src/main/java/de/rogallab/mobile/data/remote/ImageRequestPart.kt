package de.rogallab.mobile.data.remote

import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

internal fun String.isRemoteImageUrl(): Boolean =
   startsWith("http://", ignoreCase = true) ||
      startsWith("https://", ignoreCase = true)

internal fun String.toImageRequestPart(): MultipartBody.Part {
   require(!isRemoteImageUrl()) {
      "A remote image URL must not be uploaded again: $this"
   }

   val file = File(this)
   require(file.isFile) {
      "The selected image file does not exist: $this"
   }

   val contentType = when (file.extension.lowercase()) {
      "jpg", "jpeg" -> "image/jpeg"
      "png" -> "image/png"
      "webp" -> "image/webp"
      else -> "application/octet-stream"
   }.toMediaType()

   return MultipartBody.Part.createFormData(
      "file",
      file.name,
      file.asRequestBody(contentType),
   )
}

/*
 * Didaktik und Lernziele
 *
 * - Personendaten bleiben JSON. Nur eine tatsächlich lokale Bilddatei wird in
 *   einen MultipartBody.Part umgewandelt und separat übertragen.
 * - Das Part-Feld "file" entspricht exakt dem IFormFile-Parameter der API.
 * - Eine bereits vom Server gelieferte HTTP(S)-URL ist nur eine Referenz und darf
 *   nicht erneut als Datei interpretiert oder hochgeladen werden.
 */
