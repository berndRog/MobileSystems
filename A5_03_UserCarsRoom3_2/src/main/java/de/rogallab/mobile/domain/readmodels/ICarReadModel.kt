package de.rogallab.mobile.domain.readmodels

import kotlinx.coroutines.flow.Flow

// Read side for the Car table.
// Returns projections tailored to the screens; never used for writing.
interface ICarReadModel {

   // CarsScreen: all cars including the seller's full name.
   fun observeListItems(): Flow<Result<List<CarListItem>>>

   // TDriveScreen: cars for the car selection field.
   fun observeSelectItems(): Flow<Result<List<CarSelectItem>>>

   // PersonScreen (bottom sheet): cars offered by one seller.
   suspend fun findBySellerId(sellerId: String): Result<List<SellerCarItem>>
}
