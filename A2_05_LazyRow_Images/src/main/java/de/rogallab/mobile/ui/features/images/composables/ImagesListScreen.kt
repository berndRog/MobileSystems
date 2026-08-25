package de.rogallab.mobile.ui.features.images.composables

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.rogallab.mobile.domain.entities.DogImage
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.domain.utilities.AppLogger.debug
import de.rogallab.mobile.domain.utilities.AppLogger.verbose
import de.rogallab.mobile.ui.features.images.ImageIntent
import de.rogallab.mobile.ui.features.images.ImageViewModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesListScreen(
   viewModel: ImageViewModel = koinViewModel(),
   modifier: Modifier = Modifier,
) {
   val tag = "<-ImagesListScreen"

   // observe the imagesUiStateFlow in the ViewModel
   val imagesUiState by viewModel.imagesUiState.collectAsStateWithLifecycle()

   // observe the searchQuery in the ViewModel
   val query by viewModel.queryStateFlow.collectAsStateWithLifecycle()
   // observe the isSearching in the ViewModel
   val isActive by viewModel.isActiveStateFlow.collectAsStateWithLifecycle()

   // Set selectedDog to the first item in the list if it is not already set
   var selectedDog: DogImage? by remember { mutableStateOf(value = null) }
   if (selectedDog == null && imagesUiState.dogs.isNotEmpty()) {
      selectedDog = imagesUiState.dogs.first()
   }

   // read all people from repository, when the screen is created
   LaunchedEffect(Unit) {
      AppLogger.verbose(tag, "fetchDogs")
      viewModel.onProcessIntent(ImageIntent.FetchDogs)
   }

   Column(
      modifier = modifier
         .fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      DockedSearchBar(
         inputField = {
            SearchBarDefaults.InputField(
               // query text shown on SearchBar
               query = query,
               onQueryChange = { it->
                  viewModel.onProcessIntent(ImageIntent.QueryChange(it)) },
               // event, when the user triggers the Ime.Search action, i.e start filtering
               onSearch = { it: String ->
                  viewModel.onProcessIntent(ImageIntent.SearchDog(it))
                  selectedDog = viewModel.imagesUiState.value.dogs.firstOrNull()
               },
               // is searching activated
               expanded =  isActive,       //  = false when we use a single line search
               onExpandedChange = { it ->  // = {} not needed when we use a single line search
                  viewModel.onProcessIntent(ImageIntent.ActiveChange(it)) },
               // message and icon
               placeholder = { Text("Bitte geben Sie den Namen ein") },
               leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            )
         },
         expanded = isActive,              // = false when we use a single line search
         onExpandedChange = { it ->
            viewModel.onProcessIntent(ImageIntent.ActiveChange(it)) },
         modifier = Modifier.fillMaxWidth(),
         content = { },
      )
      SelectLazyRow(
         modifier = Modifier.padding(vertical = 16.dp),
         dogs = imagesUiState.dogs,
         onDogSelect = { it: DogImage ->
            selectedDog = it
            AppLogger.debug(tag,"dog clicked: ${it.name}")
         }
      )

      // local interaction with the selected dog
      selectedDog?.let { it ->
         AppLogger.debug(tag,"selectedDog: ${selectedDog?.name}")
         ImageItem(
            dog = it,
            onClick = {},
            modifier = Modifier
               .size(width = 200.dp, height = 220.dp),
         )
      }
   }
}