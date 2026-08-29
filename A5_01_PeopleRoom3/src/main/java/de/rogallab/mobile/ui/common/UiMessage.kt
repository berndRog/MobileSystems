package de.rogallab.mobile.ui.common

data class UiMessage(
   val id: Long,
   val text: UiText,
   val actionLabel: UiText? = null,
)
