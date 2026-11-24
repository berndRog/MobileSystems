package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveEffect
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveLoadFailure
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveIntent
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TDriveAdapter(
   viewModel: TDriveViewModel,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   validator: TDriveValidator = koinInject(),
) {
   val tDriveUiState by viewModel.stateFlow.collectAsStateWithLifecycle()
   val loadFailure = tDriveUiState.loadFailure
   val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is TDriveEffect.ShowMessage -> onMessage(effect.message)
         is TDriveEffect.ShowError -> onError(effect.message)
         is TDriveEffect.NavigateBack -> onNavigateBack(effect.reason)
      }
   }

   // Consume Back here so both gestures and buttons pass the ViewModel's save guard.
   NavigationBackHandler(
      state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
      isBackEnabled = lifecycleState.isAtLeast(Lifecycle.State.RESUMED),
      onBackCompleted = { viewModel.onIntent(TDriveIntent.Cancel) },
   )

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            title = {
               Text(stringResource(
                     if (tDriveUiState.isNew) R.string.test_drive_create_title
                     else R.string.test_drive_edit_title))
            },
            navigationIcon = {
               IconButton(
                  enabled = !tDriveUiState.isLoading && !tDriveUiState.isSaving,
                  onClick = {
                     viewModel.onIntent(if (loadFailure == null) TDriveIntent.Save else TDriveIntent.Cancel)
                  },
               ) {
                  Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(if (loadFailure == null) R.string.action_save else R.string.action_back))
               }
            },
         )
      },
   ) { innerPadding ->
      if (tDriveUiState.isLoading) {
         Box(
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
            contentAlignment = Alignment.TopCenter,
         ) {
            CircularProgressIndicator(modifier = Modifier.size(64.dp))
         }
      }
      else if (loadFailure != null) {
         Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
         ) {
            Text(text = loadFailure.message)
            Button(onClick = {
               viewModel.onIntent(
                  if (loadFailure is TDriveLoadFailure.NotFound) TDriveIntent.Cancel
                  else TDriveIntent.RetryLoad
               )
            }) {
               Text(stringResource(
                  if (loadFailure is TDriveLoadFailure.NotFound) R.string.action_back
                  else R.string.action_retry
               ))
            }
         }
      }
      else {
         TDriveScreen(
            tDriveUiState = tDriveUiState,
            validator = validator,
            onIntent = viewModel::onIntent,
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding)
               .padding(horizontal = 16.dp)
               .verticalScroll(rememberScrollState())
               .imePadding(),
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - TDriveAdapter enthält analog zu PersonAdapter und CarAdapter den Scaffold
 *   der Detailansicht.
 * - TopAppBar, Loading und Bottom-Navigation bleiben damit aus
 *   dem zustandslosen TDriveScreen heraus.
 */
