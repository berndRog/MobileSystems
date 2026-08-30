package de.rogallab.mobile.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.SeedDatabase
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.IImageEdit
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(): Module = module {

   val tag = "<-appModule"

   // Room-3 infrastructure belongs to A5_01 itself.
   Alog.i(tag, "single    -> AppDatabase")
   single<AppDatabase> {
      Room.databaseBuilder<AppDatabase>(
         context = androidContext(),
         name = Globals.databaseName,
      )
         .setDriver(BundledSQLiteDriver())
         .setQueryCoroutineContext(Dispatchers.IO)
         .build()
   }

   Alog.i(tag, "single    -> IPersonDao")
   single<IPersonDao> {
      get<AppDatabase>().personDao()
   }

   Alog.i(tag, "single    -> Seed")
   single<Seed> {
      Seed(
         _imageFileStorage = get<IImageFileStorage>(),
      )
   }

   Alog.i(tag, "single    -> SeedDatabase")
   single<SeedDatabase> {
      SeedDatabase(
         _personDao = get<IPersonDao>(),
         _database = get<AppDatabase>(),
         _seed = get<Seed>(),
      )
   }

   Alog.i(tag, "single    -> PersonRepository: IPersonRepository")
   single<IPersonRepository> {
      PersonRepository(
         _personDao = get<IPersonDao>(),
      )
   }

   Alog.i(tag, "single    -> PersonValidator")
   single<PersonValidator> {
      PersonValidator(
         context = androidContext(),
      )
   }

   Alog.i(tag, "viewModel -> PersonViewModel")
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

   Alog.i(tag, "viewModel -> PeopleViewModel")
   viewModel {
      PeopleViewModel(
         _repository = get<IPersonRepository>(),
         _stringProvider = get(),
         _effectDelegate =
            get<EffectDelegate<PeopleEffect>>(peopleEffectQualifier),
      )
   }
}

/*
 * Didaktik und Lernziele
 *
 * - A5_01_PeopleRoom3 übernimmt die aktuelle UI- und ViewModel-Architektur aus
 *   A4_01. Der neue Lernschritt ist ausschließlich die lokale Room-3-Schicht.
 *
 * - AppDatabase, IPersonDao, PersonDto und PersonRepository gehören deshalb zum
 *   A5_01-Modul. Ein databaseModule aus Shared wird nicht verwendet.
 *
 * - Die Datenbankkonfiguration entspricht weiterhin dem Kursstandard:
 *   BundledSQLiteDriver und ein eigener IO-Kontext für Room-Abfragen.
 *
 * - Allgemeine Dienste wie IImageFileStorage, IImageEdit und IStringProvider
 *   bleiben Shared-Infrastruktur und werden weiterhin per DI bezogen.
 *
 * - PeopleViewModel behält die einfache Delete-Bestätigung aus A4_01. A5_01
 *   übernimmt bewusst nicht den Undo-Zustand aus A4_02.
 *
 * Lernziele:
 *
 * - Room 3 innerhalb einer Data-Schicht strukturieren.
 * - DAO/DTO und Domain-Modell über Repository und Mapping entkoppeln.
 * - Bestehende ViewModels gegen eine neue Persistenzimplementierung weiterverwenden.
 */
