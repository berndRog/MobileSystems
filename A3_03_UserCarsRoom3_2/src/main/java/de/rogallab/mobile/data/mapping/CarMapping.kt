package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.domain.entities.Car

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
