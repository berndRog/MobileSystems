package de.rogallab.mobile.data.remote

import java.io.File
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

private val TEXT_MEDIA_TYPE = "text/plain".toMediaType()

internal fun String.toTextPart(): RequestBody =
   toRequestBody(TEXT_MEDIA_TYPE)

internal fun String?.toImagePartOrNull(): MultipartBody.Part? {
   val imagePath = this?.takeUnless(String::isBlank) ?: return null

   // A server URL references an already persisted image and is never uploaded.
   if (imagePath.startsWith("http://") || imagePath.startsWith("https://"))
      return null

   val file = File(imagePath)
   require(file.isFile) {
      "The selected image file does not exist: $imagePath"
   }

   val contentType = when (file.extension.lowercase()) {
      "jpg", "jpeg" -> "image/jpeg"
      "png" -> "image/png"
      "webp" -> "image/webp"
      else -> "application/octet-stream"
   }.toMediaType()

   return MultipartBody.Part.createFormData(
      "Image",
      file.name,
      file.asRequestBody(contentType),
   )
}

/*
 * Didaktik und Lernziele
 *
 * - Retrofit erwartet bei multipart/form-data einzelne RequestBody-/Multipart-
 *   Bestandteile. Diese Konvertierung wird zentral für Repository und SeedApi
 *   bereitgestellt, statt dieselbe HTTP-Technik mehrfach zu implementieren.
 * - Textfelder werden als text/plain übertragen.
 * - Nur lokale Dateipfade werden als Image-Part hochgeladen. Bereits persistierte
 *   HTTP(S)-URLs bleiben reine Referenzen auf ein Serverbild.
 */
