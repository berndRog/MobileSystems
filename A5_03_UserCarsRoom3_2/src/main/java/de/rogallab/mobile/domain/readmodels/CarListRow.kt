package de.rogallab.mobile.domain.readmodels

import androidx.room3.Embedded
import de.rogallab.mobile.data.local.dtos.CarDto

data class CarListRow(
   @Embedded val car: CarDto,
   val sellerFullName: String?,
)