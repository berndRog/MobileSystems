package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Euro
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.ui.common.toImageModel
import de.rogallab.mobile.shared.ui.images.ImageSelectionButtons
import de.rogallab.mobile.ui.cars.input_detail.CarIntent
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.MAX_CAR_IMAGE_COUNT
import de.rogallab.mobile.ui.composables.InputValueString
import de.rogallab.mobile.ui.tdrives.input_detail.comp.PersonSelectionField

@Composable
fun CarContent(
   car: Car,
   registrationYearInput: String,
   mileageInput: String,
   priceInput: String,
   people: List<Person>,
   validator: CarValidator,
   onSelectImages: () -> Unit,
   onTakePhoto: () -> Unit,
   onIntent: (CarIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   val imagePaths = car.imagePaths
   val canAddMoreImages = imagePaths.size < MAX_CAR_IMAGE_COUNT

   Column(
      modifier = modifier,
      verticalArrangement = Arrangement.spacedBy(12.dp),
   ) {
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

      InputValueString(
         value = registrationYearInput,
         onValueChange = { registrationYear ->
            onIntent(CarIntent.RegistrationYearChanged(registrationYear))
         },
         label = stringResource(R.string.car_field_registration_year),
         leadingIcon = Icons.Default.CalendarMonth,
         validate = validator::validateRegistrationYear,
         keyboardType = KeyboardType.Number,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = mileageInput,
         onValueChange = { mileage ->
            onIntent(CarIntent.MileageChanged(mileage))
         },
         label = stringResource(R.string.car_field_mileage),
         leadingIcon = Icons.Default.Speed,
         validate = validator::validateMileage,
         keyboardType = KeyboardType.Number,
         imeAction = ImeAction.Next,
      )

      InputValueString(
         value = priceInput,
         onValueChange = { price ->
            onIntent(CarIntent.PriceChanged(price))
         },
         label = stringResource(R.string.car_field_price),
         leadingIcon = Icons.Default.Euro,
         validate = validator::validatePrice,
         keyboardType = KeyboardType.Number,
         imeAction = ImeAction.Done,
      )

      PersonSelectionField(
         people = people,
         selectedPersonId = car.sellerId,
         label = stringResource(R.string.car_field_seller),
         allowNone = false,
         onPersonSelected = { personId ->
            onIntent(CarIntent.SellerChanged(personId))
         },
      )

      // The buttons come from Shared. Car only decides whether another image
      // may be added and which feature callback should be triggered.
      ImageSelectionButtons(
         imagePath = null,
         enabled = canAddMoreImages,
         onSelectPhoto = onSelectImages,
         onTakePhoto = onTakePhoto,
         onRemovePhoto = {},
      )

      Text(
         text = stringResource(
            R.string.action_manage_car_images,
            imagePaths.size,
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

      if (imagePaths.isEmpty()) {
         Text(
            text = stringResource(R.string.car_images_empty),
            style = MaterialTheme.typography.bodyMedium,
         )
      }
      else {
         CarImagePreviewList(
            imagePaths = imagePaths,
            onRemoveImage = { imagePath ->
               onIntent(CarIntent.ImageRemoved(imagePath))
            },
         )
      }
   }
}

@Composable
private fun CarImagePreviewList(
   imagePaths: List<String>,
   onRemoveImage: (String) -> Unit,
) {
   Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
   ) {
      imagePaths.forEachIndexed { imageIndex, imagePath ->
         Box(
            modifier = Modifier.fillMaxWidth(),
         ) {
            Surface(
               modifier = Modifier
                  .fillMaxWidth()
                  .height(220.dp),
               shape = RoundedCornerShape(12.dp),
            ) {
               AsyncImage(
                  // Shared contains the common String -> Coil model conversion.
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
               onClick = { onRemoveImage(imagePath) },
            ) {
               Surface(
                  shape = CircleShape,
                  color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
               ) {
                  Icon(
                     imageVector = Icons.Default.Close,
                     contentDescription =
                        stringResource(R.string.action_remove_car_image),
                     modifier = Modifier.padding(6.dp),
                  )
               }
            }
         }
      }
   }
}

/*
 * Didaktik und Lernziele
 *
 * - CarContent enthält keine ActivityResult- oder Dateioperationen mehr.
 * - Auswahl und Kamera werden über Callbacks ausgelöst; die konkreten Picker
 *   werden im Adapter durch Shared bereitgestellt.
 * - Auch die gemeinsamen Bild-Schaltflächen und die Coil-Modellkonvertierung
 *   stammen aus Shared. Das Feature ergänzt nur die fahrzeugspezifische
 *   Mehrfachbild-Vorschau.
 */
