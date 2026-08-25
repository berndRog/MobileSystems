package de.rogallab.mobile.shared.domain.utilities

import java.util.Locale
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun newUuid(): String = Uuid.random().toString()

@OptIn(ExperimentalUuidApi::class)
fun emptyUuid(): String =
   Uuid.parse("00000000-0000-0000-0000-000000000000").toString()

// UUID is handled as String
fun String.as8(): String =
   if(this.length < 8) this
   else this.substring(0..7) + "..."


//fun emptyUuid(): String = "00000000-0000-0000-0000-000000000000"
fun createUuid(number:Int, value:Int): String =
   String.format(Locale.ROOT, "%08d-%04d-0000-0000-000000000000", number, value)
