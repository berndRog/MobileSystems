package de.rogallab.mobile.shared.data.network

import java.io.IOException

sealed class NetworkException(
   message: String,
   cause: Throwable? = null,
) : IOException(message, cause)

class NoNetworkConnectionException(
   message: String = "No active network connection",
) : NetworkException(message)

class NoInternetConnectionException(
   message: String = "No validated internet connection",
) : NetworkException(message)

class NetworkTimeoutException(
   message: String,
   cause: Throwable? = null,
) : NetworkException(message, cause)

class ServerUnreachableException(
   message: String,
   cause: Throwable? = null,
) : NetworkException(message, cause)

class HttpRequestException(
   message: String,
   cause: Throwable? = null,
) : NetworkException(message, cause)

class NetworkRequestException(
   message: String,
   cause: Throwable? = null,
) : NetworkException(message, cause)

// Use a shared network message only for classified network errors.
// Other failures keep the feature-specific fallback supplied by the ViewModel.
fun Throwable.userMessageOr(fallback: String): String =
   (this as? NetworkException)
      ?.message
      ?.takeUnless(String::isBlank)
      ?: fallback

/*
 * Didaktik und Lernziele
 *
 * - Technische Netzwerkfehler werden in Shared durch eine gemeinsame
 *   NetworkException-Hierarchie beschrieben.
 * - Die ViewModels müssen dadurch weder OkHttp-, Retrofit- noch Android-
 *   Netzwerkklassen kennen.
 * - userMessageOr(...) gibt nur für klassifizierte Netzwerkfehler eine zentrale
 *   Meldung zurück. Für alle anderen Fehler bleibt der fachliche Fallback des
 *   jeweiligen Features erhalten.
 */
