package de.rogallab.mobile.ui.cars.input_detail.comp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import de.rogallab.mobile.domain.utilities.normalizedImagePaths
import de.rogallab.mobile.ui.cars.input_detail.CarIntent
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.MAX_CAR_IMAGE_COUNT
import de.rogallab.mobile.ui.common.toImageModel
import de.rogallab.mobile.ui.composables.ImageSelectionButtons
import de.rogallab.mobile.ui.composables.InputValueString
import de.rogallab.mobile.ui.composables.rememberImagePickerHandler
import de.rogallab.mobile.ui.tdrives.input_detail.comp.PersonSelectionField

@Composable
fun CarContent(
   car: Car,
   registrationYearInput: String,
   mileageInput: String,
   priceInput: String,
   people: List<Person>,
   validator: CarValidator,
   onIntent: (CarIntent) -> Unit,
   modifier: Modifier = Modifier,
) {
   val validImagePaths = car.imagePaths.normalizedImagePaths()
   val canAddMoreImages = validImagePaths.size < MAX_CAR_IMAGE_COUNT
   val remainingSlots = (MAX_CAR_IMAGE_COUNT - validImagePaths.size).coerceAtLeast(1)

   val carImagePicker = rememberImagePickerHandler(
      maxSelection = remainingSlots,
      onImagesSelected = { imagePaths ->
         onIntent(CarIntent.ImagesAdded(imagePaths))
      },
      onStorageFailed = { message ->
         onIntent(CarIntent.ImageStorageFailed(message))
      },
   )

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

//      Row(
//         modifier = Modifier.fillMaxWidth(),
//         horizontalArrangement = Arrangement.spacedBy(12.dp),
//      ) {
      Column(
         modifier = Modifier,
         verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
         ImageSelectionButtons(
            modifier = Modifier,
            imagePath = null, // no single "active" image for cars
            onSelectPhoto = {
               if (canAddMoreImages) carImagePicker.openGalleryPicker()
            },
            onTakePhoto = {
               if (canAddMoreImages) carImagePicker.openCamera()
            },
            onRemovePhoto = { },
         )


            Text(
               text = stringResource(
                  R.string.action_manage_car_images,
                  validImagePaths.size,
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
            if (!canAddMoreImages) {
               Text(
                  text = stringResource(R.string.car_images_limit, MAX_CAR_IMAGE_COUNT),
                  style = MaterialTheme.typography.bodySmall,
               )
            }
         }


      if (validImagePaths.isEmpty()) {
         Text(
            text = stringResource(R.string.car_images_empty),
            style = MaterialTheme.typography.bodyMedium,
         )
      } else {
         CarImagePreviewList(
            imagePaths = validImagePaths,
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
                     contentDescription = stringResource(R.string.action_remove_car_image),
                     modifier = Modifier.padding(6.dp),
                  )
               }
            }
         }
      }
   }
}
//package de.rogallab.mobile.ui.cars.input_detail.comp
//
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.result.PickVisualMediaRequest
//import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
//import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
//import androidx.activity.result.contract.ActivityResultContracts.TakePicture
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.imePadding
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.AddPhotoAlternate
//import androidx.compose.material.icons.filled.CalendarMonth
//import androidx.compose.material.icons.filled.CameraAlt
//import androidx.compose.material.icons.filled.Close
//import androidx.compose.material.icons.filled.Collections
//import androidx.compose.material.icons.filled.DirectionsCar
//import androidx.compose.material.icons.filled.Euro
//import androidx.compose.material.icons.filled.Speed
//import androidx.compose.material3.Button
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.ModalBottomSheet
//import androidx.compose.material3.OutlinedButton
//import androidx.compose.material3.Surface
//import androidx.compose.material3.Text
//import androidx.compose.material3.rememberModalBottomSheetState
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.rememberCoroutineScope
//import androidx.compose.runtime.saveable.rememberSaveable
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.stringResource
//import androidx.compose.ui.text.input.ImeAction
//import androidx.compose.ui.text.input.KeyboardType
//import androidx.compose.ui.unit.dp
//import coil3.compose.AsyncImage
//import de.rogallab.mobile.R
//import de.rogallab.mobile.data.local.io.copyImageToAppStorage
//import de.rogallab.mobile.data.local.io.createCameraImageFile
//import de.rogallab.mobile.data.local.io.deleteImageFromAppStorage
//import de.rogallab.mobile.domain.entities.Car
//import de.rogallab.mobile.domain.entities.Person
//import de.rogallab.mobile.domain.utilities.normalizedImagePaths
//import de.rogallab.mobile.ui.cars.input_detail.CarIntent
//import de.rogallab.mobile.ui.cars.input_detail.CarValidator
//import de.rogallab.mobile.ui.cars.input_detail.MAX_CAR_IMAGE_COUNT
//import de.rogallab.mobile.ui.common.toImageModel
//import de.rogallab.mobile.ui.common.uiText
//import de.rogallab.mobile.ui.composables.InputValueString
//import de.rogallab.mobile.ui.tdrives.input_detail.comp.PersonSelectionField
//import kotlinx.coroutines.launch
//
//// Shared editable vehicle form used by Create and Edit.
////
//// The image button opens a ModalBottomSheet from the bottom. The gallery
//// launcher supports multiple images, while the camera adds one image at a time.
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CarContent(
//   car: Car,
//   registrationYearInput: String,
//   mileageInput: String,
//   priceInput: String,
//   people: List<Person>,
//   validator: CarValidator,
//   onIntent: (CarIntent) -> Unit,
//   modifier: Modifier = Modifier,
//) {
//   val context = LocalContext.current
//   val coroutineScope = rememberCoroutineScope()
//   val imageSaveError = uiText(R.string.error_image_save)
//   val imageSheetState = rememberModalBottomSheetState(
//      skipPartiallyExpanded = true,
//   )
//
//   var showImageSheet by rememberSaveable { mutableStateOf(false) }
//   var pendingCameraImagePath by rememberSaveable { mutableStateOf<String?>(null) }
//
//   val validImagePaths = car.imagePaths.normalizedImagePaths()
//
//   val galleryPicker = rememberLauncherForActivityResult(
//      contract = PickMultipleVisualMedia(maxItems = MAX_CAR_IMAGE_COUNT ),
//   ) { selectedUris ->
//      if (selectedUris.isEmpty()) return@rememberLauncherForActivityResult
//
//      coroutineScope.launch {
//         val newImagePaths = mutableListOf<String>()
//         var copyFailed = false
//
//         selectedUris.forEach { selectedUri ->
//            copyImageToAppStorage(context = context, sourceUri = selectedUri)
//               .onSuccess { imagePath -> newImagePaths.add(imagePath) }
//               .onFailure { copyFailed = true }
//         }
//
//         if (newImagePaths.isNotEmpty()) {
//            onIntent(CarIntent.ImagesAdded(newImagePaths))
//         }
//         if (copyFailed) {
//            onIntent(CarIntent.ImageStorageFailed(imageSaveError))
//         }
//      }
//   }
//
//   val camera = rememberLauncherForActivityResult(
//      contract = TakePicture(),
//   ) { photoWasTaken ->
//      val imagePath = pendingCameraImagePath
//      pendingCameraImagePath = null
//
//      when {
//         photoWasTaken && imagePath != null -> {
//            onIntent(CarIntent.ImagesAdded(listOf(imagePath)))
//         }
//
//         imagePath != null -> {
//            deleteImageFromAppStorage(imagePath)
//         }
//      }
//   }
//
//   Column(
//      modifier = modifier,
//      verticalArrangement = Arrangement.spacedBy(12.dp),
//   ) {
//      InputValueString(
//         value = car.manufacturer,
//         onValueChange = { manufacturer ->
//            onIntent(CarIntent.ManufacturerChanged(manufacturer))
//         },
//         label = stringResource(R.string.car_field_manufacturer),
//         leadingIcon = Icons.Default.DirectionsCar,
//         validate = validator::validateManufacturer,
//         imeAction = ImeAction.Next,
//      )
//
//      InputValueString(
//         value = car.model,
//         onValueChange = { model ->
//            onIntent(CarIntent.ModelChanged(model))
//         },
//         label = stringResource(R.string.car_field_model),
//         leadingIcon = Icons.Default.DirectionsCar,
//         validate = validator::validateModel,
//         imeAction = ImeAction.Next,
//      )
//
//      InputValueString(
//         value = registrationYearInput,
//         onValueChange = { registrationYear ->
//            onIntent(CarIntent.RegistrationYearChanged(registrationYear))
//         },
//         label = stringResource(R.string.car_field_registration_year),
//         leadingIcon = Icons.Default.CalendarMonth,
//         validate = validator::validateRegistrationYear,
//         keyboardType = KeyboardType.Number,
//         imeAction = ImeAction.Next,
//      )
//
//      InputValueString(
//         value = mileageInput,
//         onValueChange = { mileage ->
//            onIntent(CarIntent.MileageChanged(mileage))
//         },
//         label = stringResource(R.string.car_field_mileage),
//         leadingIcon = Icons.Default.Speed,
//         validate = validator::validateMileage,
//         keyboardType = KeyboardType.Number,
//         imeAction = ImeAction.Next,
//      )
//
//      InputValueString(
//         value = priceInput,
//         onValueChange = { price ->
//            onIntent(CarIntent.PriceChanged(price))
//         },
//         label = stringResource(R.string.car_field_price),
//         leadingIcon = Icons.Default.Euro,
//         validate = validator::validatePrice,
//         keyboardType = KeyboardType.Number,
//         imeAction = ImeAction.Done,
//      )
//
//      PersonSelectionField(
//         people = people,
//         selectedPersonId = car.sellerId,
//         label = stringResource(R.string.car_field_seller),
//         allowNone = false,
//         onPersonSelected = { personId ->
//            onIntent(CarIntent.SellerChanged(personId))
//         },
//      )
//
//      OutlinedButton(
//         modifier = Modifier.fillMaxWidth(),
//         onClick = { showImageSheet = true },
//      ) {
//         Icon(
//            imageVector = Icons.Default.Collections,
//            contentDescription = null,
//         )
//         Text(
//            modifier = Modifier.padding(start = 8.dp),
//            text = stringResource(
//               R.string.action_manage_car_images,
//               validImagePaths.size,
//            ),
//         )
//      }
//
//      if (validImagePaths.isEmpty()) {
//         Text(
//            text = stringResource(R.string.car_images_empty),
//            style = MaterialTheme.typography.bodyMedium,
//         )
//      } else {
//         CarImagePreviewList(
//            imagePaths = validImagePaths,
//            onRemoveImage = { imagePath ->
//               onIntent(CarIntent.ImageRemoved(imagePath))
//            },
//         )
//      }
//   }
//
//   if (showImageSheet) {
//      ModalBottomSheet(
//         onDismissRequest = { showImageSheet = false },
//         sheetState = imageSheetState,
//      ) {
//         Column(
//            modifier = Modifier
//               .fillMaxWidth()
//               .imePadding()
//               .padding(
//                  start = 24.dp,
//                  end = 24.dp,
//                  bottom = 32.dp,
//               ),
//            verticalArrangement = Arrangement.spacedBy(12.dp),
//         ) {
//            Text(
//               text = stringResource(R.string.car_images_sheet_title),
//               style = MaterialTheme.typography.titleLarge,
//            )
//
//            Button(
//               modifier = Modifier.fillMaxWidth(),
//               enabled = validImagePaths.size < MAX_CAR_IMAGE_COUNT,
//               onClick = {
//                  showImageSheet = false
//                  galleryPicker.launch(
//                     PickVisualMediaRequest(PickVisualMedia.ImageOnly)
//                  )
//               },
//            ) {
//               Icon(
//                  imageVector = Icons.Default.AddPhotoAlternate,
//                  contentDescription = null,
//               )
//               Text(
//                  modifier = Modifier.padding(start = 8.dp),
//                  text = stringResource(R.string.action_select_car_images),
//               )
//            }
//
//            Button(
//               modifier = Modifier.fillMaxWidth(),
//               enabled = validImagePaths.size < MAX_CAR_IMAGE_COUNT,
//               onClick = {
//                  showImageSheet = false
//                  createCameraImageFile(context)
//                     .onSuccess { cameraImage ->
//                        pendingCameraImagePath = cameraImage.imagePath
//                        camera.launch(cameraImage.contentUri)
//                     }
//                     .onFailure {
//                        onIntent(CarIntent.ImageStorageFailed(imageSaveError))
//                     }
//               },
//            ) {
//               Icon(
//                  imageVector = Icons.Default.CameraAlt,
//                  contentDescription = null,
//               )
//               Text(
//                  modifier = Modifier.padding(start = 8.dp),
//                  text = stringResource(R.string.action_take_photo),
//               )
//            }
//
//            Text(
//               text = stringResource(
//                  R.string.car_images_limit,
//                  MAX_CAR_IMAGE_COUNT,
//               ),
//               style = MaterialTheme.typography.bodySmall,
//            )
//         }
//      }
//   }
//}
//
//@Composable
//private fun CarImagePreviewList(
//   imagePaths: List<String>,
//   onRemoveImage: (String) -> Unit,
//) {
//   Column(
//      modifier = Modifier.fillMaxWidth(),
//      verticalArrangement = Arrangement.spacedBy(12.dp),
//   ) {
//      imagePaths.forEachIndexed { imageIndex, imagePath ->
//         Box(
//            modifier = Modifier.fillMaxWidth(),
//         ) {
//            Surface(
//               modifier = Modifier
//                  .fillMaxWidth()
//                  .height(220.dp),
//               shape = RoundedCornerShape(12.dp),
//            ) {
//               AsyncImage(
//                  model = imagePath.toImageModel(),
//                  contentDescription = stringResource(
//                     R.string.car_image_preview_numbered,
//                     imageIndex + 1,
//                  ),
//                  contentScale = ContentScale.Crop,
//                  modifier = Modifier.fillMaxWidth(),
//               )
//            }
//
//            IconButton(
//               modifier = Modifier.align(Alignment.TopEnd),
//               onClick = { onRemoveImage(imagePath) },
//            ) {
//               Surface(
//                  shape = CircleShape,
//                  color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
//               ) {
//                  Icon(
//                     imageVector = Icons.Default.Close,
//                     contentDescription = stringResource(
//                        R.string.action_remove_car_image,
//                     ),
//                     modifier = Modifier.padding(6.dp),
//                  )
//               }
//            }
//         }
//      }
//   }
//}
