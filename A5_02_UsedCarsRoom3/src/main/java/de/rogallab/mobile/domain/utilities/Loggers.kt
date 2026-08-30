package de.rogallab.mobile.domain.utilities

import android.util.Log

// Central logger whose backend can be switched for local JVM tests.
object AppLogger {
   private const val TAG = "AppLogger"

   private const val DEFAULT_USE_ANDROID_LOG = true
   private const val DEFAULT_IS_VERBOSE = true
   private const val DEFAULT_IS_DEBUG = true
   private const val DEFAULT_IS_INFO = true
   private const val DEFAULT_IS_COMP = true

   private var _useAndroidLog = DEFAULT_USE_ANDROID_LOG
   private var _isVerbose = DEFAULT_IS_VERBOSE
   private var _isDebug = DEFAULT_IS_DEBUG
   private var _isInfo = DEFAULT_IS_INFO
   private var _isComp = DEFAULT_IS_COMP

   // Changes only settings whose argument is not null.
   fun set(
      useAndroidLog: Boolean? = null,
      isVerbose: Boolean? = null,
      isDebug: Boolean? = null,
      isInfo: Boolean? = null,
      isComp: Boolean? = null,
   ) {
      _useAndroidLog = useAndroidLog ?: _useAndroidLog
      _isVerbose = isVerbose ?: _isVerbose
      _isDebug = isDebug ?: _isDebug
      _isInfo = isInfo ?: _isInfo
      _isComp = isComp ?: _isComp
   }

   // Restores the normal Android application configuration.
   fun reset() {
      _useAndroidLog = DEFAULT_USE_ANDROID_LOG
      _isVerbose = DEFAULT_IS_VERBOSE
      _isDebug = DEFAULT_IS_DEBUG
      _isInfo = DEFAULT_IS_INFO
      _isComp = DEFAULT_IS_COMP
   }

   // Disables all output, for example during local unit tests.
   fun disable() {
      _isVerbose = false
      _isDebug = false
      _isInfo = false
      _isComp = false
   }

   fun verbose(
      tag: String = TAG,
      message: String = "",
   ) {
      if (!_isVerbose) return
      if (_useAndroidLog)
         Log.v(tag, formatMessage(message))
      else
         println("V/$tag: ${formatMessage(message)}")
   }

   fun debug(
      tag: String = TAG,
      message: String = "",
   ) {
      if (!_isDebug) return

      if (_useAndroidLog)
         Log.d(tag, formatMessage(message))
      else
         println("D/$tag: ${formatMessage(message)}")
   }

   fun info(
      tag: String = TAG,
      message: String = "",
   ) {
      if (!_isInfo) return

      if (_useAndroidLog)
         Log.i(tag, formatMessage(message))
      else
         println("I/$tag: ${formatMessage(message)}")

   }

   fun compose(
      tag: String = TAG,
      message: String = "",
   ) {
      if (!_isComp) return

      if (_useAndroidLog)
         Log.v(tag, message)
      else
         println("C/$tag: $message")

   }

   fun error(
      tag: String = TAG,
      message: String = "",
      throwable: Throwable? = null,
   ) {
      if (_useAndroidLog)
         Log.e(tag, formatMessage(message), throwable)
      else
         println("E/$tag: ${formatMessage(message)}")
         throwable?.printStackTrace()
   }

   private fun formatMessage(message: String): String =
      String.format(
         "%-110s %s",
         message,
         Thread.currentThread(),
      )
}

// Lernziele und didaktische Einordnung
// ------------------------------------
// - Die Android-App protokolliert über android.util.Log.
// - Lokale JVM-Tests können Android-Logging deaktivieren oder auf println
//   umschalten und benötigen dadurch kein Mock für android.util.Log.
// - reset() verhindert, dass die Singleton-Konfiguration zwischen Tests
//   unbeabsichtigt erhalten bleibt.
