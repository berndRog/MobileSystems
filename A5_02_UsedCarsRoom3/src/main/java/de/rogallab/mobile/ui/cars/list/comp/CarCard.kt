package de.rogallab.mobile.ui.cars.list.comp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.ui.images.ImageRenderer

@Composable
fun CarCard(
   car: Car,
   sellerName: String,
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
         ImageRenderer(
            modifier = Modifier
               .weight(1f)
               .padding(4.dp),
            imageVector = Icons.Default.DirectionsCar,
            imagePath = car.primaryImagePath,
            contentDescription = car.displayName,
         )

         Column(
            modifier = Modifier
               .weight(3f)
               .padding(horizontal = 4.dp),
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
