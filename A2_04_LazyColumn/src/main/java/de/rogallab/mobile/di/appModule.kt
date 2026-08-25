package de.rogallab.mobile.di

import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.SeedDatabase
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.data.IPersonDao
import de.rogallab.mobile.shared.data.local.database.AppDatabasePerson
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// defines the Koin module for the application,
// including dependencies for seeding the database, repositories, and view models.
//
// singletons are created for Seed, SeedDatabase, and PersonRepository
// viewmodels are provided for PersonViewModel and PeopleViewModel.
fun appModule(): Module = module {

   val tag = "<-appModules"

   Alog.i(tag, "single    -> Seed")
   single<Seed> {
      Seed()
   }

   Alog.i(tag, "single    -> SeedDatabase")
   single<SeedDatabase> {
      SeedDatabase(
         _personDao = get<IPersonDao>(),
         _database = get< AppDatabasePerson>(),
         _seed = get<Seed>()
      )
   }

   Alog.i(tag, "single    -> PersonRepository: IPersonRepository")
   single<IPersonRepository> {
      PersonRepository(
         _personDao = get<IPersonDao>()
      )
   }

   Alog.i(tag, "viewModel -> PersonViewModel")
   viewModel { parameters ->
      PersonViewModel(
         personId = parameters.getOrNull<String>(),
         _repository = get<IPersonRepository>(),
      )
   }

   Alog.i(tag, "viewModel -> PeopleViewModel")
   viewModel {
      PeopleViewModel(
         _repository = get<IPersonRepository>(),
      )
   }
}
