package de.rogallab.mobile.ui.people.create_detail

sealed interface PersonEffect {

   // Shows a short informational message in the UI.
   data class ShowMessage(val message: String) : PersonEffect

   // Shows an error message that must be acknowledged by the user.
   data class ShowError(val message: String) : PersonEffect

}

/*
 * Didaktik und Lernziele
 *
 * - Einmalige Meldungen sind Effects; A3_02 verwendet noch keinen Backstack.
 */
