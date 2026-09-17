package de.rogallab.mobile.data.repositories

import de.rogallab.mobile.data.mapping.toCar
import de.rogallab.mobile.data.mapping.toCarDto
import de.rogallab.mobile.data.remote.ICarWebservice
import de.rogallab.mobile.data.remote.isRemoteImageUrl
import de.rogallab.mobile.data.remote.toImageRequestPart
import de.rogallab.mobile.data.remote.dtos.CarDto
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.entities.Car
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import java.net.URI
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException

class CarRepository(
   private val _webservice: ICarWebservice,
   private val _networkExceptionMapper: NetworkExceptionMapper,
) : ICarRepository {
   private val _state = MutableStateFlow<Result<List<Car>>>(Result.success(emptyList()))

   override fun observeAll(): Flow<Result<List<Car>>> = flow {
      refresh()
      emitAll(_state)
   }

   override suspend fun findById(id: String): Result<Car?> =
      try {
         Result.success(_webservice.getById(id).toCar().also(::upsert))
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (exception: HttpException) {
         if (exception.code() == 404) Result.success(null)
         else Result.failure(_networkExceptionMapper.map(exception))
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }

   override suspend fun findByPersonId(personId: String): Result<List<Car>> = request {
      _webservice.getByPersonId(personId).map(CarDto::toCar)
   }

   override suspend fun create(car: Car): Result<Unit> = request {
      val localImages = car.imagePaths.filterNot(String::isRemoteImageUrl)
      localImages.forEach(String::toImageRequestPart)

      val created = _webservice.create(
         car.copy(imagePaths = emptyList()).toCarDto()
      )
      try {
         var persisted = created
         localImages.forEach { imagePath ->
            persisted = _webservice.uploadImage(
               created.id,
               imagePath.toImageRequestPart(),
            )
         }
         upsert(persisted.toCar())
      }
      catch (exception: Throwable) {
         runCatching { _webservice.delete(created.id) }
         throw exception
      }
   }

   override suspend fun update(car: Car): Result<Unit> = request {
      var persisted = _webservice.update(
         car.id,
         car.copy(imagePaths = emptyList()).toCarDto(),
      )

      val desiredRemoteUrls = car.imagePaths.filter(String::isRemoteImageUrl).toSet()
      persisted.imageUrls
         .filterNot(desiredRemoteUrls::contains)
         .forEach { imageUrl ->
            persisted = _webservice.deleteImage(
               car.id,
               requireNotNull(URI(imageUrl).path.substringAfterLast('/').takeIf(String::isNotBlank)),
            )
         }

      car.imagePaths
         .filterNot(String::isRemoteImageUrl)
         .forEach { imagePath ->
            persisted = _webservice.uploadImage(
               car.id,
               imagePath.toImageRequestPart(),
            )
         }
      upsert(persisted.toCar())
   }

   override suspend fun remove(car: Car): Result<Unit> = request {
      val response = _webservice.delete(car.id)
      if (!response.isSuccessful) throw HttpException(response)
      _state.value = Result.success(
         _state.value.getOrDefault(emptyList()).filterNot { it.id == car.id }
      )
   }

   private suspend fun refresh() {
      _state.value = request {
         sorted(_webservice.getAll().map(CarDto::toCar))
      }
   }

   private fun upsert(car: Car) {
      val cars = _state.value.getOrDefault(emptyList()).filterNot { it.id == car.id } + car
      _state.value = Result.success(sorted(cars))
   }

   private fun sorted(cars: List<Car>) = cars.sortedWith(
      compareBy<Car> { it.manufacturer.lowercase() }.thenBy { it.model.lowercase() }
   )

   private suspend fun <T> request(block: suspend () -> T): Result<T> =
      try {
         Result.success(block())
      }
      catch (exception: CancellationException) {
         throw exception
      }
      catch (throwable: Throwable) {
         Result.failure(_networkExceptionMapper.map(throwable))
      }
}
