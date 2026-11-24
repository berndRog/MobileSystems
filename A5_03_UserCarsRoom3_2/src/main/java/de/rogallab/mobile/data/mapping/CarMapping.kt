package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.data.readmodels.CarListRow
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.readmodels.CarListItem
import de.rogallab.mobile.domain.readmodels.CarSelectItem
import de.rogallab.mobile.domain.readmodels.SellerCarItem

// Write side: persistence model <-> domain entity.
fun CarDto.toCar(): Car = Car(
   id = id,
   manufacturer = manufacturer,
   model = model,
   price = price,
   imagePaths = imagePaths,
   personId = personId,
)

fun Car.toCarDto(): CarDto = CarDto(
   id = id,
   manufacturer = manufacturer,
   model = model,
   price = price,
   imagePaths = imagePaths,
   personId = personId,
)

// Read side: query results -> projections.
fun CarListRow.toCarListItem(): CarListItem = CarListItem(
   id = car.id,
   displayName = "${car.manufacturer} ${car.model}".trim(),
   primaryImagePath = car.imagePaths.firstOrNull { it.isNotBlank() },
   price = car.price,
   sellerFullName = sellerFullName,
)

fun CarDto.toSellerCarItem(): SellerCarItem = SellerCarItem(
   id = id,
   displayName = "$manufacturer $model".trim(),
   primaryImagePath = imagePaths.firstOrNull { it.isNotBlank() },
   price = price,
)

fun CarDto.toCarSelectItem(): CarSelectItem = CarSelectItem(
   id = id,
   displayName = "$manufacturer $model".trim(),
)
