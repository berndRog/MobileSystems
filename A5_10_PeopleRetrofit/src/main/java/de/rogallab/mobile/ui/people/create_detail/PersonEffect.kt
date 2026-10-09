package de.rogallab.mobile.ui.people.create_detail

enum class BackReason {
   Save,
   Cancel,
}

sealed interface PersonEffect {

   // Shows a short informational message in the UI.
   data class ShowMessage(val message: String) : PersonEffect

   // Requests an error message that disappears automatically after a long duration.
   data class ShowError(val message: String) : PersonEffect

   // Navigates back to the previous screen. Handled by the Navigation 3 layer.
   data class NavigateBack(
      val reason: BackReason,
   ) : PersonEffect
}
