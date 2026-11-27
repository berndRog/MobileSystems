package de.rogallab.mobile.di

import retrofit2.Retrofit
import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.isRemoteImageUrl
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.data.remote.Seed
import de.rogallab.mobile.data.remote.SeedApi
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
import de.rogallab.mobile.ui.people.create_detail.PersonValidator
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
      // Reuse the injected dispatcher for Retrofit list observation.
      requireNotNull(
         get<CoroutineContext>(named("dispatcherIo"))[ContinuationInterceptor] as? CoroutineDispatcher
      ) { "dispatcherIo must contain a CoroutineDispatcher" }
   }

   single<IPersonWebservice> {
      get<Retrofit>().create(IPersonWebservice::class.java)
   }

   Alog.i(tag, "single    -> Seed")
   single<Seed> {
      Seed(
         _imageFileStorage = get<IImageFileStorage>(),
      )
   }
   single<SeedApi> {
      SeedApi(_personWeb = get(), _seed = get())
   }

   Alog.i(tag, "single    -> PersonReadModel: IPersonReadModel")
   single<IPersonReadModel> {
      PersonReadModel(
         _repository = get<PersonRepository>(),
         _dispatcherIo = get(named("dispatcherIo")),
      )
   }

   Alog.i(tag, "single    -> PersonRepository: IPersonRepository")
   single<PersonRepository> {
      PersonRepository(_webservice = get(), _networkExceptionMapper = get<NetworkExceptionMapper>())
   }
   single<IPersonRepository> { get<PersonRepository>() }

   Alog.i(tag, "single    -> PersonValidator")
   single<PersonValidator> {
      PersonValidator(
         context = androidContext(),
      )
   }

   // Server URLs are owned by the REST API, never by local file storage.
   factory<IImageEdit> {
      val storage = get<IImageFileStorage>()
      de.rogallab.mobile.shared.ui.images.ImageEdit(
         object : IImageFileStorage by storage {
            override suspend fun deleteImageFromAppStorage(imagePath: String?): Result<Unit> =
               if (imagePath?.isRemoteImageUrl() == true) Result.success(Unit)
               else storage.deleteImageFromAppStorage(imagePath)
         }
      )
   }

   Alog.i(tag, "factory   -> PersonUcCreate")
   factory<PersonUcCreate> { parameters ->
      PersonUcCreate(
         _repository = get<IPersonRepository>(),
         _imageEdit = parameters.get<IImageEdit>(),
         _imageFileStorage = get<IImageFileStorage>(),
      )
   }

   Alog.i(tag, "factory   -> PersonUcUpdate")
   factory<PersonUcUpdate> { parameters ->
      PersonUcUpdate(
         _repository = get<IPersonRepository>(),
         _imageEdit = parameters.get<IImageEdit>(),
         _imageFileStorage = get<IImageFileStorage>(),
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
