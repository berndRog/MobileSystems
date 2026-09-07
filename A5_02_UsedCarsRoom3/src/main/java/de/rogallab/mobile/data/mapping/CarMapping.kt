package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.domain.entities.Car

fun CarDto.toCar(): Car = Car(
   id = id,
   manufacturer = manufacturer,
   model = model,
   registration = year,
   price = price,
   sellerId = sellerId,
   imagePaths = imagePaths,
)

fun Car.toCarDto(): CarDto = CarDto(
   id = id,
   manufacturer = manufacturer,
   model = model,
   year = registration,
   price = price,
   sellerId = requireNotNull(sellerId) {
      "A car requires a seller."
   },
   imagePaths = imagePaths,
)
