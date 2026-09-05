package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.shared.ui.common.toImageModel
import de.rogallab.mobile.shared.ui.components.InputValueString
import de.rogallab.mobile.shared.ui.images.ImageSelectionButtons
import de.rogallab.mobile.ui.cars.input_detail.CarIntent
import de.rogallab.mobile.ui.cars.input_detail.CarUiState
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.MAX_CAR_IMAGE_COUNT
import de.rogallab.mobile.ui.tdrives.input_detail.comp.PersonSelectionField

private const val TAG = "<-CarScreen"

@Composable
fun CarScreen(
   carUiState: CarUiState,
   validator: CarValidator,
   onSelectImages: () -> Unit,
   onTakePhoto: () -> Unit,
   onIntent: (CarIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   var compositionCount by remember { mutableIntStateOf(0) }
   SideEffect {
      AppLogger.compose(TAG, "Composition #${compositionCount++}")
   }

   val car = carUiState.car ?: return

   LazyColumn(
      modifier = modifier.imePadding(),
      contentPadding = PaddingValues(
         start = 16.dp,
         top = 8.dp,
         end = 16.dp,
         bottom = 24.dp,
      ),
      verticalArrangement = Arrangement.spacedBy(12.dp),
   ) {
      item(key = "manufacturer") {
         InputValueString(
            value = car.manufacturer,
            onValueChange = { manufacturer ->
               onIntent(CarIntent.ManufacturerChanged(manufacturer))
            },
            label = stringResource(R.string.car_field_manufacturer),
            leadingIcon = Icons.Default.DirectionsCar,
            validate = validator::validateManufacturer,
            imeAction = ImeAction.Next,
         )
      }

      item(key = "model") {
         InputValueString(
            value = car.model,
            onValueChange = { model ->
               onIntent(CarIntent.ModelChanged(model))
            },
            label = stringResource(R.string.car_field_model),
            leadingIcon = Icons.Default.DirectionsCar,
            validate = validator::validateModel,
            imeAction = ImeAction.Next,
         )
      }

      item(key = "registrationYear") {
         InputValueString(
            value = carUiState.registrationYearInput,
            onValueChange = { registrationYear ->
               onIntent(CarIntent.RegistrationYearChanged(registrationYear))
            },
            label = stringResource(R.string.car_field_registration_year),
            leadingIcon = Icons.Default.CalendarMonth,
            validate = validator::validateRegistrationYear,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
         )
      }

      item(key = "mileage") {
         InputValueString(
            value = carUiState.mileageInput,
            onValueChange = { mileage ->
               onIntent(CarIntent.MileageChanged(mileage))
            },
            label = stringResource(R.string.car_field_mileage),
            leadingIcon = Icons.Default.Speed,
            validate = validator::validateMileage,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Next,
         )
      }

      item(key = "price") {
         InputValueString(
            value = carUiState.priceInput,
            onValueChange = { price ->
               onIntent(CarIntent.PriceChanged(price))
            },
            label = stringResource(R.string.car_field_price),
            leadingIcon = Icons.Default.Euro,
            validate = validator::validatePrice,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
         )
      }

      item(key = "seller") {
         PersonSelectionField(
            people = carUiState.people,
            selectedPersonId = car.sellerId,
            label = stringResource(R.string.car_field_seller),
            allowNone = false,
            onPersonSelected = { personId ->
               onIntent(CarIntent.SellerChanged(personId))
            },
         )
      }

      item(key = "imageActions") {
         Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
         ) {
            ImageSelectionButtons(
               imagePath = null,
               enabled = car.imagePaths.size < MAX_CAR_IMAGE_COUNT,
               onSelectPhoto = onSelectImages,
               onTakePhoto = onTakePhoto,
               onRemovePhoto = {},
            )

            Text(
               text = stringResource(
                  R.string.action_manage_car_images,
                  car.imagePaths.size,
               ),
               style = MaterialTheme.typography.bodyMedium,
            )

            Text(
               text = stringResource(
                  R.string.car_images_limit,
                  MAX_CAR_IMAGE_COUNT,
               ),
               style = MaterialTheme.typography.bodySmall,
            )
         }
      }

      if (car.imagePaths.isEmpty()) {
         item(key = "emptyImages") {
            Text(
               text = stringResource(R.string.car_images_empty),
               style = MaterialTheme.typography.bodyMedium,
            )
         }
      }
      else {
         itemsIndexed(
            items = car.imagePaths,
            key = { _, imagePath -> imagePath },
         ) { imageIndex, imagePath ->
            CarImagePreview(
               imagePath = imagePath,
               imageIndex = imageIndex,
               onRemoveImage = {
                  onIntent(CarIntent.ImageRemoved(imagePath))
               },
            )
         }
      }

      item(key = "actions") {
         Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(
               40.dp,
               Alignment.CenterHorizontally,
            ),
         ) {
            OutlinedButton(onClick = { onIntent(CarIntent.Cancel) }) {
               Text(text = stringResource(R.string.action_cancel))
            }

            Button(onClick = { onIntent(CarIntent.Save) }) {
               Text(text = stringResource(R.string.action_save))
            }
         }
      }
   }
}

@Composable
private fun CarImagePreview(
   imagePath: String,
   imageIndex: Int,
   onRemoveImage: () -> Unit,
) {
   Box(modifier = Modifier.fillMaxWidth()) {
      Surface(
         modifier = Modifier
            .fillMaxWidth()
            .height(220.dp),
         shape = RoundedCornerShape(12.dp),
      ) {
         AsyncImage(
            model = imagePath.toImageModel(),
            contentDescription = stringResource(
               R.string.car_image_preview_numbered,
               imageIndex + 1,
            ),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(),
         )
      }

      IconButton(
         modifier = Modifier.align(Alignment.TopEnd),
         onClick = onRemoveImage,
      ) {
         Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
         ) {
            Icon(
               imageVector = Icons.Default.Close,
               contentDescription = stringResource(R.string.action_remove_car_image),
               modifier = Modifier.padding(6.dp),
            )
         }
      }
   }
}
