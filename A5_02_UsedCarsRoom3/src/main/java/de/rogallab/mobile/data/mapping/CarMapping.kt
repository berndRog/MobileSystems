package de.rogallab.mobile.data.mapping

import de.rogallab.mobile.data.local.dtos.CarDto
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.domain.utilities.normalizedImagePaths

fun CarDto.toCar(): Car = Car(
   id = id,
   manufacturer = manufacturer,
   model = model,
   registrationYear = registrationYear,
   mileage = mileage,
   priceInEuro = priceInEuro,
   sellerId = sellerId,
   imagePaths = imagePaths.normalizedImagePaths(),
)

fun Car.toCarDto(): CarDto = CarDto(
   id = id,
   manufacturer = manufacturer,
   model = model,
   registrationYear = registrationYear,
   mileage = mileage,
   priceInEuro = priceInEuro,
   sellerId = requireNotNull(sellerId) { "A car requires a seller." },
   imagePaths = imagePaths.normalizedImagePaths(),
)
