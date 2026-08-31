package de.rogallab.mobile.domain.utilities


fun String.max(n:Int): String {
   val end = Math.min(this.length, n)
   val result = this.substring(0, end)
   return result
}