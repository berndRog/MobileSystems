package de.rogallab.mobile.ui.people.list

sealed interface PeopleEffect {
   data class ShowMessage(val message: String) : PeopleEffect
   data class ShowError(val message: String) : PeopleEffect
}
