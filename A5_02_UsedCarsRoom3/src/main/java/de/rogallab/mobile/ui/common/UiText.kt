package de.rogallab.mobile.ui.common

import android.content.Context
import androidx.annotation.StringRes

// Represents user-visible text without resolving Android resources in a ViewModel.
sealed interface UiText {

   // References a string resource and optional formatting arguments.
   data class Resource(
      @StringRes val resourceId: Int,
      val arguments: List<Any> = emptyList(),
   ) : UiText

   // Holds text that has already been resolved from a resource.
   // This is used for validation functions that currently return String values.
   data class Resolved(
      val value: String,
   ) : UiText
}

// Resolves user-visible text at the UI boundary where a Context is available.
fun UiText.resolve(context: Context): String =
   when (this) {
      is UiText.Resource -> context.getString(
         resourceId,
         *arguments.toTypedArray(),
      )

      is UiText.Resolved -> value
   }

// Creates a resource-backed UI text value with optional formatting arguments.
fun uiText(
   @StringRes resourceId: Int,
   vararg arguments: Any,
): UiText = UiText.Resource(
   resourceId = resourceId,
   arguments = arguments.toList(),
)
