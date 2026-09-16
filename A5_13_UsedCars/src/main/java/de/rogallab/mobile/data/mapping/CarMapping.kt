package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.remote.dtos.CarDto
import de.rogallab.mobile.domain.entities.Car

fun CarDto.toCar(): Car = Car(
   id = id,
   manufacturer = manufacturer,
   model = model,
   price = price,
   imagePaths = imageUrls,
   personId = personId,
)

fun Car.toCarDto(): CarDto = CarDto(
   id = id,
   manufacturer = manufacturer,
   model = model,
   price = price,
   imageUrls = imagePaths,
   personId = requireNotNull(personId) { "A car requires a seller." },
)
