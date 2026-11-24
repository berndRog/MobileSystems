package de.rogallab.mobile.ui.cars.input_detail

import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.ui.cars.input_detail.comp.CarForm

// Domain -> form: a price becomes text for the TextField.
fun Car.toCarForm(): CarForm = CarForm(
   id = id,
   manufacturer = manufacturer,
   model = model,
   priceInput = price?.toString().orEmpty(),
   imagePaths = imagePaths,
   personId = personId,
)

// Normalizes user input before validation. Stays a CarForm,
// so the UI can show the cleaned values.
fun CarForm.normalized(): CarForm = copy(
   manufacturer = manufacturer.trim(),
   model = model.trim(),
   priceInput = priceInput.trim(),
)

// Form -> domain: call only after successful validation.
fun CarForm.toCar(): Car = Car(
   id = id,
   manufacturer = manufacturer,
   model = model,
   price = priceInput.toIntOrNull(),
   imagePaths = imagePaths,
   personId = personId,
)
