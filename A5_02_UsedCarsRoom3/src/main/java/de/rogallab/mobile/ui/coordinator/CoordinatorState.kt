package de.rogallab.mobile.ui.coordinator

import de.rogallab.mobile.ui.common.UiMessage

data class CoordinatorState(
   val message: UiMessage? = null,
   val isWriting: Boolean = false,
)
