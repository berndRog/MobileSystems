package de.rogallab.mobile

/** Artificial delays for demonstrations; tests can supply shorter durations. */
data class AppTiming(
   val delayRead: Long = Globals.delayRead,
   val delayWrite: Long = Globals.delayWrite,
)
