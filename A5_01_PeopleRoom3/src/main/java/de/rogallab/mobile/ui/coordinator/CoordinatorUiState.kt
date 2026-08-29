package de.rogallab.mobile.ui.coordinator

import de.rogallab.mobile.ui.common.UiMessage

data class PeopleCoordinatorState(
   val message: UiMessage? = null,
   val isWriting: Boolean = false
)