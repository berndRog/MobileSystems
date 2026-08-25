package de.rogallab.mobile.ui.people.create_detail.comp

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.components.collectAsStateWithLifecycleLogs
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonIntent
import de.rogallab.mobile.ui.people.create_detail.PersonUiState
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel

@Composable
fun PersonAdapter(
   viewModel: PersonViewModel,
   modifier: Modifier = Modifier,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
) {
   val tag = "<-PersonAdapter"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.intValue++}") }

   val personUiState: PersonUiState
      by viewModel.stateFlow.collectAsStateWithLifecycle()

   EffectHandler(viewModel.effects) { personEffect ->
      when (personEffect) {
         is PersonEffect.ShowMessage -> onMessage(personEffect.message)
         is PersonEffect.ShowError -> onError(personEffect.message)
         is PersonEffect.NavigateBack -> onNavigateBack(personEffect.reason)
      }
   }

   val person = personUiState.person

   PersonScreen(
      isNew = personUiState.isNew,
      isLoading = personUiState.isLoading,

      firstName = person.firstName,
      onFirstNameChange = { viewModel.onIntent(PersonIntent.FirstNameChange(it)) },

      lastName = person.lastName,
      onLastNameChange = { viewModel.onIntent(PersonIntent.LastNameChange(it)) },

      email = person.email,
      onEmailChange = { viewModel.onIntent(PersonIntent.EmailChange(it)) },

      phone = person.phone,
      onPhoneChange = { viewModel.onIntent(PersonIntent.PhoneChange(it)) },

      imagePath = person.imagePath,
      onImagePathChange = { viewModel.onIntent(PersonIntent.ImagePathChange(it)) },
      onImageStorageFailed = { viewModel.onIntent(PersonIntent.ImageStorageFailed(it)) },

      onNavigateBack = { viewModel.onIntent(PersonIntent.Cancel) },
      onSave = { viewModel.onIntent(PersonIntent.Save) },
      onCancel = { viewModel.onIntent(PersonIntent.Cancel) },

      modifier = modifier
         .fillMaxSize()
         .verticalScroll(rememberScrollState())
         .imePadding()
         .fillMaxWidth(),
   )
}

/*
 * Didaktik und Lernziele
 *
 * - Der Adapter bleibt die Verbindung zwischen ViewModel und zustandslosem
 *   PersonScreen. A3_04 ergänzt nur zwei neue UI-Ereignisse:
 *
 *      onImagePathChange      -> PersonIntent.ImagePathChange
 *      onImageStorageFailed   -> PersonIntent.ImageStorageFailed
 *
 * - ImagePickerHandler bleibt in der UI-Schicht. Die Lebensdauer der gespeicherten
 *   Bilddateien wird im ViewModel an ImageEditDelegate aus shared_01 delegiert.
 *
 * - Die bereits in A3_03 eingeführten Message-, Error- und Navigation-Effects
 *   bleiben unverändert.
 *
 * Lernziele:
 *
 * - Neue UI-Ereignisse über vorhandene Adapter-Strukturen ergänzen.
 * - Bildauswahl und Effect-Verarbeitung voneinander getrennt halten.
 */
