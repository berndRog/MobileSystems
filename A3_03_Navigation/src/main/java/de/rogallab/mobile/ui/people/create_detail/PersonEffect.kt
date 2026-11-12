package de.rogallab.mobile.ui.people.create_detail

enum class BackReason {
   Save,
   Cancel,
}

sealed interface PersonEffect {
   data class ShowMessage(val message: String) : PersonEffect
   data class ShowError(val message: String) : PersonEffect

   data class NavigateBack(val reason: BackReason) : PersonEffect
}
