package de.rogallab.mobile.domain.entities

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class Person(
   val firstName: String = "",
   val lastName: String = "",
   val id: String = UUID.randomUUID().toString()
)
