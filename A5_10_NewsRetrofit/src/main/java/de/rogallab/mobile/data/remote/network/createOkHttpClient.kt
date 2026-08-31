package de.rogallab.mobile.data.remote.network

import de.rogallab.mobile.domain.utilities.logDebug
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

fun createOkHttpClient(
   bearerToken: BearerTokenInterceptor,
   apiKey: ApiKeyInterceptor,
   networkConnectivity: ConnectivityInterceptor,
   loggingInterceptor: HttpLoggingInterceptor
) : OkHttpClient {
   logDebug("<-OkHttpClient", "create()")
   return OkHttpClient.Builder()
      .connectTimeout(30, TimeUnit.SECONDS)
      .readTimeout(5, TimeUnit.SECONDS)
      .writeTimeout(5, TimeUnit.SECONDS)
      .addInterceptor(bearerToken)
      .addInterceptor(apiKey)
      .addInterceptor(networkConnectivity)
      .addInterceptor(loggingInterceptor)
      .build()
}
