package de.rogallab.mobile.data.remote.network

import okhttp3.Interceptor
import okhttp3.Response

class ApiKeyInterceptor(
   private val _apiKey: String,
) : Interceptor {
   override fun intercept(chain: Interceptor.Chain): Response {
      val originalRequest = chain.request()
      if (_apiKey.isBlank()) return chain.proceed(originalRequest)

      val request = originalRequest.newBuilder()
         .header("X-Api-Key", _apiKey)
         .build()

      return chain.proceed(request)
   }
}
