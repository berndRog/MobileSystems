package de.rogallab.mobile.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import de.rogallab.mobile.BuildConfig
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.remote.ICarWebservice
import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.ITDriveWebservice
import de.rogallab.mobile.data.remote.InstantTypeAdapter
import de.rogallab.mobile.data.remote.Seed
import de.rogallab.mobile.data.remote.SeedApi
import de.rogallab.mobile.data.repositories.CarRepository
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.data.repositories.TDriveRepository
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.IImageEdit
import de.rogallab.mobile.ui.cars.input_detail.CarEffect
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.CarViewModel
import de.rogallab.mobile.ui.cars.list.CarsEffect
import de.rogallab.mobile.ui.cars.list.CarsViewModel
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveEffect
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import de.rogallab.mobile.ui.tdrives.list.TDrivesEffect
import de.rogallab.mobile.ui.tdrives.list.TDrivesViewModel
import kotlin.time.Instant
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun appModule(): Module = module {
   val tag = "<-appModule"

   Alog.i(tag, "single    -> HttpLoggingInterceptor")
   single<HttpLoggingInterceptor> {
      HttpLoggingInterceptor().apply {
         level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
         else HttpLoggingInterceptor.Level.NONE
      }
   }

   single<OkHttpClient> {
      OkHttpClient.Builder()
         .addInterceptor(get<HttpLoggingInterceptor>())
         .build()
   }

   single<Gson> {
      GsonBuilder()
         .registerTypeAdapter(Instant::class.java, InstantTypeAdapter())
         .create()
   }

   single<Retrofit> {
      Retrofit.Builder()
         .baseUrl(Globals.baseUrl)
         .client(get<OkHttpClient>())
         .addConverterFactory(GsonConverterFactory.create(get<Gson>()))
         .build()
   }

   single<IPersonWebservice> { get<Retrofit>().create(IPersonWebservice::class.java) }
   single<ICarWebservice> { get<Retrofit>().create(ICarWebservice::class.java) }
   single<ITDriveWebservice> { get<Retrofit>().create(ITDriveWebservice::class.java) }

   single<IPersonRepository> { PersonRepository(get<IPersonWebservice>()) }
   single<ICarRepository> { CarRepository(get<ICarWebservice>()) }
   single<ITDriveRepository> { TDriveRepository(get<ITDriveWebservice>()) }

   single { PersonValidator(context = androidContext()) }
   single { CarValidator(context = androidContext()) }
   single { TDriveValidator(context = androidContext()) }

   single { Seed(_imageFileStorage = get<IImageFileStorage>()) }
   single {
      SeedApi(
         _personWeb = get<IPersonWebservice>(),
         _carWeb = get<ICarWebservice>(),
         _tDriveWeb = get<ITDriveWebservice>(),
         _imageFileStorage = get<IImageFileStorage>(),
         _seed = get<Seed>(),
      )
   }

   viewModel { parameters ->
      PersonViewModel(
         personId = parameters.getOrNull<String>(),
         _repository = get<IPersonRepository>(),
         _carRepository = get<ICarRepository>(),
         _stringProvider = get(),
         _validator = get<PersonValidator>(),
         _imageFileStorage = get<IImageFileStorage>(),
         _imageEdit = get<IImageEdit>(),
         _effectDelegate = get<EffectDelegate<PersonEffect>>(personEffectQualifier),
      )
   }

   viewModel {
      PeopleViewModel(
         _repository = get<IPersonRepository>(),
         _stringProvider = get(),
         _effectDelegate = get<EffectDelegate<PeopleEffect>>(peopleEffectQualifier),
      )
   }

   viewModel { parameters ->
      CarViewModel(
         carId = parameters.getOrNull<String>(),
         _carRepository = get<ICarRepository>(),
         _personRepository = get<IPersonRepository>(),
         _stringProvider = get(),
         _validator = get<CarValidator>(),
         _imageFileStorage = get<IImageFileStorage>(),
         _imageEdit = get<IImageEdit>(),
         _effectDelegate = get<EffectDelegate<CarEffect>>(carEffectQualifier),
      )
   }

   viewModel {
      CarsViewModel(
         _repository = get<ICarRepository>(),
         _personRepository = get<IPersonRepository>(),
         _stringProvider = get(),
         _effectDelegate = get<EffectDelegate<CarsEffect>>(carsEffectQualifier),
      )
   }

   viewModel { parameters ->
      TDriveViewModel(
         tDriveId = parameters.getOrNull<String>(),
         _tDriveRepository = get<ITDriveRepository>(),
         _personRepository = get<IPersonRepository>(),
         _carRepository = get<ICarRepository>(),
         _stringProvider = get(),
         _validator = get<TDriveValidator>(),
         _effectDelegate = get<EffectDelegate<TDriveEffect>>(tDriveEffectQualifier),
      )
   }

   viewModel {
      TDrivesViewModel(
         _tDriveRepository = get<ITDriveRepository>(),
         _personRepository = get<IPersonRepository>(),
         _carRepository = get<ICarRepository>(),
         _stringProvider = get(),
         _effectDelegate = get<EffectDelegate<TDrivesEffect>>(tDrivesEffectQualifier),
      )
   }
}
