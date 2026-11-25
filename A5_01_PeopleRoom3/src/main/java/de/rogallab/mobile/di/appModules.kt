package de.rogallab.mobile.di

import de.rogallab.mobile.AppTiming
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.SeedDatabase
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.images.ImageSelectionMode
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import de.rogallab.mobile.data.readmodels.PersonReadModel
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext

fun appModule(
   dispatcherIo: CoroutineContext = Dispatchers.IO
): Module = module {

   val tag = "<-appModule"

   Alog.i(tag, "single    -> CoroutineContext mit Dispatcher.IO")
   single { AppTiming() }

   Alog.i(tag, "single    -> CoroutineContext mit Dispatcher.IO")
   single<CoroutineContext>(named("dispatcherIo")) {
      dispatcherIo
   }
   single<CoroutineDispatcher>(named("dispatcherIo")) {
      // Keep the full context for Room and reuse its dispatcher for flowOn.
      requireNotNull(
         get<CoroutineContext>(named("dispatcherIo"))[ContinuationInterceptor] as? CoroutineDispatcher
      ) { "dispatcherIo must contain a CoroutineDispatcher" }
   }

   // Room-3 infrastructure belongs to A5_01 itself.
   Alog.i(tag, "single    -> AppDatabase")
   single<AppDatabase> {
      Room.databaseBuilder<AppDatabase>(
         context = androidContext(),
         name = Globals.databaseName,
      )
      .setDriver(BundledSQLiteDriver())
      .setQueryCoroutineContext(dispatcherIo)
      .addCallback(get<SeedDatabase>())
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
         _seed = get<Seed>(),
      )
   }

   single<IPersonReadModel> {
      PersonReadModel(
         _personDao = get(),
         _dispatcherIo = get(named("dispatcherIo")),
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

   factory<PersonUcCreate> { parameters ->
      PersonUcCreate(
         _repository = get<IPersonRepository>(),
         _imageEdit = parameters.get<IImageEdit>(),
      )
   }

   factory<PersonUcUpdate> { parameters ->
      PersonUcUpdate(
         _repository = get<IPersonRepository>(),
         _imageEdit = parameters.get<IImageEdit>(),
      )
   }

   Alog.i(tag, "viewModel -> PersonViewModel")
   viewModel { parameters ->
      // Both save use cases must share the ViewModel's image edit session.
      val imageEdit = get<IImageEdit>()

      PersonViewModel(
         personId = parameters.getOrNull<String>(),
         _repository = get<IPersonRepository>(),
         _stringProvider = get<IStringProvider>(),
         _validator = get<PersonValidator>(),
         _images = ImageEditDelegate(
            session = imageEdit,
            storage = get<IImageFileStorage>(),
            mode = ImageSelectionMode.REPLACE,
            maxImages = 1,
         ),
         _personUcCreate = get<PersonUcCreate> {
            parametersOf(imageEdit)
         },
         _personUcUpdate = get<PersonUcUpdate> {
            parametersOf(imageEdit)
         },
         _timing = get<AppTiming>(),
         _effectDelegate = get<EffectDelegate<PersonEffect>>(personEffectQualifier),
      )
   }

   Alog.i(tag, "factory   -> PersonUcDelete")
   factory<PersonUcDelete> {
      PersonUcDelete(
         _repository = get<IPersonRepository>(),
         _imageFileStorage = get<IImageFileStorage>()
      )
   }

   Alog.i(tag, "viewModel -> PeopleViewModel")
   viewModel {
      PeopleViewModel(
         _readModel = get<IPersonReadModel>(),
         _personUcDelete = get<PersonUcDelete>(),
         _stringProvider = get<IStringProvider>(),
         _timing = get<AppTiming>(),
         _effectDelegate = get<EffectDelegate<PeopleEffect>>(peopleEffectQualifier),
      )
   }
}
