package de.rogallab.mobile.shared.data.network

import android.content.Context
import de.rogallab.mobile.shared.R
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

class NetworkExceptionMapper(
   context: Context,
) {

   private val _context: Context = context.applicationContext

   fun map(throwable: Throwable): Throwable =
      when (throwable) {
         is NoNetworkConnectionException ->
            NoNetworkConnectionException(
               _context.getString(R.string.error_no_network_connection)
            )

         is NoInternetConnectionException ->
            NoInternetConnectionException(
               _context.getString(R.string.error_no_internet_connection)
            )

         is NetworkException ->
            throwable

         is SocketTimeoutException ->
            NetworkTimeoutException(
               message = _context.getString(R.string.error_network_timeout),
               cause = throwable,
            )

         is ConnectException,
         is UnknownHostException ->
            ServerUnreachableException(
               message = _context.getString(R.string.error_server_unreachable),
               cause = throwable,
            )

         is HttpException ->
            HttpRequestException(
               message = _context.getString(
                  R.string.error_http_request,
                  throwable.code(),
               ),
               cause = throwable,
            )

         is IOException ->
            NetworkRequestException(
               message = _context.getString(R.string.error_network_request),
               cause = throwable,
            )

         else -> throwable
      }
}

/*
 * Didaktik und Lernziele
 *
 * - NetworkExceptionMapper übersetzt technische Fehler aus Android, OkHttp und
 *   Retrofit in wenige gemeinsame NetworkException-Typen.
 * - Die zugehörigen Benutzertexte liegen zentral als String-Ressourcen in Shared.
 * - HTTP-Fehler, Timeouts und Verbindungsfehler werden dadurch nicht in jedem
 *   Repository oder ViewModel erneut ausgewertet.
 * - Unbekannte, nicht netzwerkbezogene Fehler werden unverändert weitergereicht.
 */
