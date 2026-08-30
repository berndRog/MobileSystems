package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.ui.common.toImageModel
import de.rogallab.mobile.ui.composables.ImageRenderer
import kotlin.text.isBlank

@Composable
fun CarCard(
   car: Car,
   sellerName: String
) {
   val notAvailable = stringResource(R.string.value_not_available)
   val registrationYear = car.registrationYear?.toString() ?: notAvailable
   val mileage = car.mileage?.toString() ?: notAvailable
   val price = car.priceInEuro?.toString() ?: notAvailable

   Card(
      modifier = Modifier
         .fillMaxWidth()
         .height(90.dp),
      shape = RoundedCornerShape(12.dp),
   ) {
      Row(
         modifier = Modifier.fillMaxSize(),
         verticalAlignment = Alignment.CenterVertically,
      ) {

         val imagePath = car.primaryImagePath

//         Surface(
//            modifier = Modifier
//               .weight(0.25f)
//               .padding(4.dp)
//               .fillMaxHeight(),
//            shape = RoundedCornerShape(10.dp)
//         ) {
//            if (imagePath.isNullOrBlank()) {
//               Icon(
//                  imageVector = Icons.Default.DirectionsCar,
//                  contentDescription = car.displayName,
//               )
//            } else {
//               AsyncImage(
//                  model = imagePath.toImageModel(),
//                  contentDescription = car.displayName,
//                  contentScale = ContentScale.Crop
//               )
//            }
//         }

         ImageRenderer(
            modifier = Modifier
               .weight(1f)
               .padding(4.dp),
            imageVector = Icons.Default.AccountCircle,
            imagePath = imagePath,
            contentDescription = car.displayName
         )


         Column(
            modifier = Modifier
               .weight(3.0f)
               .padding(horizontal = 4.dp)
         ) {
            Text(
               text = car.displayName,
               style = MaterialTheme.typography.titleMedium,
            )
            if (sellerName.isNotBlank()) {
               Text(
                  text = sellerName,
                  style = MaterialTheme.typography.bodyMedium,
               )
            }
            Text(
               text = stringResource(
                  R.string.car_card_details,
                  registrationYear,
                  mileage,
                  price,
               ),
               style = MaterialTheme.typography.bodyMedium,
            )
         }
      }
   }
}