package de.rogallab.mobile.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.data.local.database.migration1To2
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
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(): Module = module {
   val tag = "<-appModule"

   Alog.i(tag, "single    -> AppDatabase")
   single<AppDatabase> {
      Room.databaseBuilder<AppDatabase>(
         context = androidContext(),
         name = Globals.databaseName,
      )
         .setDriver(BundledSQLiteDriver())
         .setQueryCoroutineContext(Dispatchers.IO)
         .addMigrations(migration1To2)
         .build()
   }

   single<IPersonDao> { get<AppDatabase>().createPersonDao() }
   single<ICarDao> { get<AppDatabase>().createCarDao() }
   single<ITDriveDao> { get<AppDatabase>().createTDriveDao() }

   single<IPersonRepository> { PersonRepository(get()) }
   single<ICarRepository> { CarRepository(get()) }
   single<ITDriveRepository> { TDriveRepository(get()) }

   single { PersonValidator(context = androidContext()) }
   single { CarValidator(context = androidContext()) }
   single { TDriveValidator(context = androidContext()) }

   single {
      SeedDatabase(
         _personDao = get(),
         _carDao = get(),
         _tDriveDao = get(),
         _imageFileStorage = get<IImageFileStorage>(),
      )
   }

   viewModel { parameters ->
      PersonViewModel(
         personId = parameters.getOrNull<String>(),
         _repository = get<IPersonRepository>(),
         _stringProvider = get(),
         _validator = get<PersonValidator>(),
         _imageFileStorage = get<IImageFileStorage>(),
         _imageEdit = get<IImageEdit>(),
         _effectDelegate =
            get<EffectDelegate<PersonEffect>>(personEffectQualifier),
      )
   }

   viewModel {
      PeopleViewModel(
         _repository = get<IPersonRepository>(),
         _stringProvider = get(),
         _effectDelegate =
            get<EffectDelegate<PeopleEffect>>(peopleEffectQualifier),
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
         _effectDelegate =
            get<EffectDelegate<CarEffect>>(carEffectQualifier),
      )
   }

   viewModel {
      CarsViewModel(
         _repository = get<ICarRepository>(),
         _personRepository = get<IPersonRepository>(),
         _stringProvider = get(),
         _effectDelegate =
            get<EffectDelegate<CarsEffect>>(carsEffectQualifier),
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
         _effectDelegate =
            get<EffectDelegate<TDriveEffect>>(tDriveEffectQualifier),
      )
   }

   viewModel {
      TDrivesViewModel(
         _tDriveRepository = get<ITDriveRepository>(),
         _personRepository = get<IPersonRepository>(),
         _carRepository = get<ICarRepository>(),
         _stringProvider = get(),
         _effectDelegate =
            get<EffectDelegate<TDrivesEffect>>(tDrivesEffectQualifier),
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - AppDatabase, die drei DAOs und die drei Repository-Implementierungen gehören
 *   vollständig zum A5_02-Modul. Shared liefert allgemeine Infrastruktur.
 *
 * - Dazu zählen ausdrücklich auch IImageFileStorage und IImageEdit. Personen
 *   und Fahrzeuge verwenden dieselben Shared-Dienste; A5_02 implementiert keine
 *   eigene Datei- oder ImagePicker-Infrastruktur.
 *
 * - SeedDatabase erhält IImageFileStorage ebenfalls per Dependency Injection.
 *   Dadurch verwendet der Seed für Personenbilder dieselbe Datei-Infrastruktur
 *   wie die eigentliche Anwendung.
 *
 * - Jeder Feature-Bereich besitzt einen eigenen EffectDelegate. Ein globales
 *   CoordinatorViewModel ist weder für Navigation noch für Meldungen notwendig.
 */
