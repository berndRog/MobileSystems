package de.rogallab.mobile.ui.features.images

import androidx.compose.runtime.Immutable
import de.rogallab.mobile.domain.entities.DogImage

@Immutable
data class ImagesUiState (
   val dogs: List<DogImage> = emptyList(),
)