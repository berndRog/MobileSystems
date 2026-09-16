package de.rogallab.mobile.shared.di

import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.core.module.Module
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@OptIn(ExperimentalSerializationApi::class)
fun networkModule(
   baseUrl: String,
   isDebug: Boolean,
): Module = module {

   val tag = "<-networkModule"

   Alog.i(tag, "single    -> HttpLoggingInterceptor")
   single<HttpLoggingInterceptor> {
      HttpLoggingInterceptor().apply {
         level = if (isDebug) {
            HttpLoggingInterceptor.Level.BASIC
         }
         else {
            HttpLoggingInterceptor.Level.NONE
         }
      }
   }

   Alog.i(tag, "single    -> OkHttpClient")
   single<OkHttpClient> {
      OkHttpClient.Builder()
         .addInterceptor(get<HttpLoggingInterceptor>())
         .build()
   }

   Alog.i(tag, "single    -> Json")
   single<Json> {
      Json {
         ignoreUnknownKeys = true
         explicitNulls = false
         coerceInputValues = true
      }
   }

   Alog.i(tag, "single    -> Retrofit")
   single<Retrofit> {
      val json: Json = get()

      Retrofit.Builder()
         .baseUrl(baseUrl)
         .client(get<OkHttpClient>())
         .addConverterFactory(
            json.asConverterFactory(
               "application/json".toMediaType()
            )
         )
         .build()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - Die technische Netzwerk-Infrastruktur ist nicht an eine konkrete REST-API
 *   gebunden und kann deshalb von mehreren Vorlesungsprojekten verwendet werden.
 *
 * - HttpLoggingInterceptor protokolliert Requests nur in Debug-Builds.
 * - OkHttpClient führt die HTTP-Kommunikation aus.
 * - Json konfiguriert kotlinx.serialization für die JSON-Konvertierung.
 * - Retrofit verbindet Base-URL, OkHttpClient und JSON-Converter.
 *
 * - Die projektspezifische Webservice-Schnittstelle bleibt außerhalb von Shared.
 *   Retrofit erzeugt daraus im jeweiligen Projekt den Client für den Webservice.
 */
