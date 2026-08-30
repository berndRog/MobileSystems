package de.rogallab.mobile.di

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.data.local.database.migration1To2
import de.rogallab.mobile.data.repositories.TDriveRepository
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.data.repositories.CarRepository
import de.rogallab.mobile.domain.ITDriveRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.ICarRepository
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveVmArgs
import de.rogallab.mobile.ui.tdrives.list.TDrivesViewModel
import de.rogallab.mobile.ui.people.input_detail.PersonValidator
import de.rogallab.mobile.ui.people.input_detail.PersonViewModel
import de.rogallab.mobile.ui.people.input_detail.PersonVmArgs
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.coordinator.CoordinatorViewModel
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.CarViewModel
import de.rogallab.mobile.ui.cars.input_detail.CarVmArgs
import de.rogallab.mobile.ui.cars.list.CarsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {

   single<CoroutineScope> {
      CoroutineScope(SupervisorJob() + Dispatchers.IO)
   }

   single<AppDatabase> {
      Room.databaseBuilder(
         androidContext(),
         AppDatabase::class.java,
         Globals.databaseName,
      )
         .setDriver(AndroidSQLiteDriver())
         .addMigrations(migration1To2)
         .build()
   }

   single<IPersonDao> { get<AppDatabase>().createPersonDao() }
   single<ICarDao> { get<AppDatabase>().createCarDao() }
   single<ITDriveDao> { get<AppDatabase>().createTDriveDao() }

   single<IPersonRepository> {
      PersonRepository(_personDao = get())
   }
   single<ICarRepository> {
      CarRepository(_carDao = get())
   }
   single<ITDriveRepository> {
      TDriveRepository(_tDriveDao = get())
   }

   single { PersonValidator(context = androidContext()) }
   single { CarValidator(context = androidContext()) }
   single { TDriveValidator(context = androidContext()) }

   single {
      SeedDatabase(
         _personDao = get(),
         _carDao = get(),
         _tDriveDao = get(),
         _applicationScope = get(),
      )
   }


   viewModel {
      CoordinatorViewModel(
         _personRepository = get(),
         _carRepository = get(),
         _tDriveRepository = get(),
      )
   }

   viewModel { PeopleViewModel(_repository = get()) }
   viewModel {
      CarsViewModel(
         _repository = get(),
         _personRepository = get(),
      )
   }
   viewModel {
      TDrivesViewModel(
         _tDriveRepository = get(),
         _personRepository = get(),
         _carRepository = get(),
      )
   }

   viewModel { parameters ->
      val arguments: PersonVmArgs = parameters.get()
      PersonViewModel(
         arguments = arguments,
         _repository = get(),
         _validator = get(),
      )
   }

   viewModel { parameters ->
      val arguments: CarVmArgs = parameters.get()
      CarViewModel(
         arguments = arguments,
         _carRepository = get(),
         _personRepository = get(),
         _validator = get(),
      )
   }

   viewModel { parameters ->
      val arguments: TDriveVmArgs = parameters.get()
      TDriveViewModel(
         arguments = arguments,
         _tDriveRepository = get(),
         _personRepository = get(),
         _carRepository = get(),
         _validator = get(),
      )
   }
}
