package de.rogallab.mobile.domain.utilities

import android.util.Log

object AppLogger {
   private const val TAG = "AppLogger"
   private var _useAndroidLog = true
   private var _isVerbose = true
   private var _isDebug = true
   private var _isInfo = true
   private var _isComp = true

   fun set(
      useAndroidLog: Boolean?,
      isVerbose: Boolean?,
      isDebug: Boolean?,
      isInfo: Boolean?,
      isComp: Boolean?
   ) {
      _useAndroidLog = useAndroidLog ?: _useAndroidLog
      _isVerbose = isVerbose ?: _isVerbose
      _isDebug = isDebug ?: _isDebug
      _isInfo = isInfo ?: _isInfo
      _isComp = isComp ?: _isComp

   }

   fun verbose(tag: String = TAG, message: String = "") {
      if (_useAndroidLog && _isVerbose)
         Log.v(tag, message)
      else if (_isVerbose)
         println("V/$tag: $message")
   }

   fun debug(tag: String = TAG, message: String = "") {
      if (_useAndroidLog && _isDebug)
         Log.d(tag, formatMessage(message))
      else if (_isDebug)
         println("D/$tag: ${formatMessage(message)}")
   }

   fun info(tag: String = TAG, message: String = "") {
      if (_useAndroidLog && _isInfo)
         Log.d(tag, formatMessage(message))
      else if (_isInfo)
         println("I/$tag: ${formatMessage(message)}")
   }

   fun AppLogger.compose(tag: String, message: String) {
      if (_useAndroidLog && _isComp)
         Log.d(tag, formatMessage(message))
      else if (_isComp)
         println("C/$tag: ${formatMessage(message)}")
   }

   fun error(tag: String = TAG, message: String = "", throwable: Throwable? = null) {
      if (_useAndroidLog)
         Log.e(tag, message, throwable)
      else {
         println("E/$tag: $message")
         throwable?.printStackTrace()
      }
   }

   private fun formatMessage(message: String) =
      String.format("%-110s %s", message, Thread.currentThread().toString())

}
