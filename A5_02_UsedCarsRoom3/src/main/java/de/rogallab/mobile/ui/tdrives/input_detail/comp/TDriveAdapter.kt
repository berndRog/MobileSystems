package de.rogallab.mobile.ui.tdrives.input_detail.comp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.R
import de.rogallab.mobile.shared.ui.effects.EffectHandler
import de.rogallab.mobile.ui.people.create_detail.BackReason
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveEffect
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveIntent
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TDriveAdapter(
   viewModel: TDriveViewModel,
   snackbarHostState: SnackbarHostState,
   bottomBar: @Composable () -> Unit,
   onMessage: (String) -> Unit,
   onError: (String) -> Unit,
   onNavigateBack: (BackReason) -> Unit,
   validator: TDriveValidator = koinInject(),
) {
   val tDriveUiState by viewModel.stateFlow.collectAsStateWithLifecycle()

   EffectHandler(viewModel.effects) { effect ->
      when (effect) {
         is TDriveEffect.ShowMessage -> onMessage(effect.message)
         is TDriveEffect.ShowError -> onError(effect.message)
         is TDriveEffect.NavigateBack -> onNavigateBack(effect.reason)
      }
   }

   Scaffold(
      modifier = Modifier.fillMaxSize(),
      topBar = {
         TopAppBar(
            title = {
               Text(
                  stringResource(
                     if (tDriveUiState.isNew) R.string.test_drive_create_title
                     else R.string.test_drive_edit_title,
                  )
               )
            },
            navigationIcon = {
               IconButton(onClick = { viewModel.onIntent(TDriveIntent.Save) }) {
                  Icon(
                     imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                     contentDescription = stringResource(R.string.action_save),
                  )
               }
            },
         )
      },
      bottomBar = bottomBar,
      snackbarHost = {
         SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.imePadding(),
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
      else {
         TDriveScreen(
            tDriveUiState = tDriveUiState,
            validator = validator,
            onIntent = viewModel::onIntent,
            modifier = Modifier
               .fillMaxSize()
               .padding(innerPadding),
         )
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - TDriveAdapter enthält analog zu PersonAdapter und CarAdapter den Scaffold
 *   der Detailansicht.
 * - TopAppBar, Loading, SnackbarHost und Bottom-Navigation bleiben damit aus
 *   dem zustandslosen TDriveScreen heraus.
 */
