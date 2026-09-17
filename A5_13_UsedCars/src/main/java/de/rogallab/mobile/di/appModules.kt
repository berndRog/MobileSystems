package de.rogallab.mobile.di

import de.rogallab.mobile.data.remote.ICarWebservice
import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.ITDriveWebservice
import de.rogallab.mobile.data.remote.Seed
import de.rogallab.mobile.data.remote.SeedApi
import de.rogallab.mobile.data.repositories.CarRepository
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.data.repositories.TDriveRepository
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
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
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

fun appModule(): Module = module {
   single<IPersonWebservice> { get<Retrofit>().create(IPersonWebservice::class.java) }
   single<ICarWebservice> { get<Retrofit>().create(ICarWebservice::class.java) }
   single<ITDriveWebservice> { get<Retrofit>().create(ITDriveWebservice::class.java) }

   single<IPersonRepository> {
      PersonRepository(
         _webservice = get<IPersonWebservice>(),
         _networkExceptionMapper = get<NetworkExceptionMapper>(),
      )
   }
   single<ICarRepository> {
      CarRepository(
         _webservice = get<ICarWebservice>(),
         _networkExceptionMapper = get<NetworkExceptionMapper>(),
      )
   }
   single<ITDriveRepository> {
      TDriveRepository(
         _webservice = get<ITDriveWebservice>(),
         _networkExceptionMapper = get<NetworkExceptionMapper>(),
      )
   }

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

/*
 * Didaktik und Lernziele
 *
 * - Shared registriert den allgemeinen Netzwerk-Stack. Dieses App-Modul kennt
 *   nur die drei fachlichen Retrofit-Schnittstellen der UsedCarsApi.
 * - Person-, Car- und TDriveRepository bleiben projektspezifisch. Alle drei
 *   erhalten denselben zentralen NetworkExceptionMapper aus Shared.
 * - Dadurch verwenden Personen, Fahrzeuge und Probefahrten dieselben Timeout-,
 *   Verbindungs- und HTTP-Fehlerregeln, ohne sie dreimal zu implementieren.
 */
