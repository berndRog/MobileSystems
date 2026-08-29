package de.rogallab.mobile.di

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.ui.coordinator.PeopleCoordinatorViewModel
import de.rogallab.mobile.ui.people.input_detail.PersonValidator
import de.rogallab.mobile.ui.people.input_detail.PersonViewModel
import de.rogallab.mobile.ui.people.input_detail.PersonVmArgs
import de.rogallab.mobile.ui.people.list.PeopleViewModel
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
         Globals.databaseName
      )
         // Room 3 no longer uses SupportSQLite internally. A SQLiteDriver is
         // therefore configured explicitly.
         .setDriver(AndroidSQLiteDriver())
         .build()
   }

   single<IPersonDao> {
      get<AppDatabase>().createPersonDao()
   }

   single<IPersonRepository> {
      PersonRepository(
         _personDao = get()
      )
   }

   single {
      PersonValidator(
         context = androidContext()
      )
   }

   single {
      SeedDatabase(
         _personDao = get(),
         _applicationScope = get()
      )
   }


   viewModel {
      PeopleCoordinatorViewModel(
         _repository = get()
      )
   }

   viewModel {
      PeopleViewModel(
         _repository = get()
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
}
