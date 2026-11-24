package de.rogallab.mobile.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.ICarDao
import de.rogallab.mobile.data.IPersonDao
import de.rogallab.mobile.data.ITDriveDao
import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.database.AppDatabase
import de.rogallab.mobile.data.local.database.SeedDatabase
import de.rogallab.mobile.data.local.database.migration1To2
import de.rogallab.mobile.data.readmodels.CarReadModel
import de.rogallab.mobile.data.readmodels.PersonReadModel
import de.rogallab.mobile.data.readmodels.TDriveReadModel
import de.rogallab.mobile.data.repositories.CarRepository
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.data.repositories.TDriveRepository
import de.rogallab.mobile.domain.readmodels.ICarReadModel
import de.rogallab.mobile.domain.repositories.ICarRepository
import de.rogallab.mobile.domain.repositories.IPersonRepository
import de.rogallab.mobile.domain.repositories.ITDriveRepository
import de.rogallab.mobile.domain.readmodels.IPersonReadModel
import de.rogallab.mobile.domain.readmodels.ITDriveReadModel
import de.rogallab.mobile.domain.usecases.CarUcCreate
import de.rogallab.mobile.domain.usecases.CarUcDelete
import de.rogallab.mobile.domain.usecases.CarUcUpdate
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.domain.usecases.TDriveUcDelete
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.images.ImageSelectionMode
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.cars.input_detail.CarEffect
import de.rogallab.mobile.ui.cars.input_detail.CarValidator
import de.rogallab.mobile.ui.cars.input_detail.CarViewModel
import de.rogallab.mobile.ui.cars.input_detail.MAX_CAR_IMAGE_COUNT
import de.rogallab.mobile.ui.cars.list.CarsEffect
import de.rogallab.mobile.ui.cars.list.CarsViewModel
import de.rogallab.mobile.ui.people.create_detail.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveEffect
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveValidator
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveViewModel
import de.rogallab.mobile.ui.tdrives.list.TDrivesEffect
import de.rogallab.mobile.ui.tdrives.list.TDrivesViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
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
   single<CoroutineContext>(named("dispatcherIo")) { dispatcherIo }
   single<CoroutineDispatcher>(named("dispatcherIo")) {
      // Keep the full context for Room and reuse its dispatcher for flowOn.
      requireNotNull(
         get<CoroutineContext>(named("dispatcherIo"))[ContinuationInterceptor] as? CoroutineDispatcher
      ) { "dispatcherIo must contain a CoroutineDispatcher" }
   }

   Alog.i(tag, "single    -> AppDatabase")
   single<AppDatabase> {
      Room.databaseBuilder<AppDatabase>(
         context = androidContext(),
         name = Globals.databaseName,
      )
         .setDriver(BundledSQLiteDriver())
         .setQueryCoroutineContext(dispatcherIo)
         .addMigrations(migration1To2)
         .build()
   }


   Alog.i(tag, "single    -> IPersonDao")
   single<IPersonDao> { get<AppDatabase>().createPersonDao() }

   Alog.i(tag, "single    -> ICarDao")
   single<ICarDao> { get<AppDatabase>().createCarDao() }

   Alog.i(tag, "single    -> ITDriveDao")
   single<ITDriveDao> { get<AppDatabase>().createTDriveDao() }

   single { Seed(_imageFileStorage = get<IImageFileStorage>()) }

   single {
      SeedDatabase(
         _personDao = get<IPersonDao>(),
         _carDao = get<ICarDao>(),
         _tDriveDao = get(),
         _imageFileStorage = get<IImageFileStorage>(),
         _seed = get<Seed>()

      )
   }

   Alog.i(tag, "single    -> IPersonReadModel")
   single<IPersonReadModel> {
      PersonReadModel(
         _personDao = get<IPersonDao>(),
         _dispatcherIo = get<CoroutineDispatcher>(named("dispatcherIo"))
      )
   }
   Alog.i(tag, "single    -> IPersonRepository")
   single<IPersonRepository> {
      PersonRepository(
         _personDao = get<IPersonDao>(),
         _dispatcherIo = get<CoroutineDispatcher>(named("dispatcherIo"))
      )
   }
   Alog.i(tag, "single    -> ICarReadModel")
   single<ICarReadModel> {
      CarReadModel(
         _carDao = get<ICarDao>(),
         _dispatcherIo = get<CoroutineDispatcher>(named("dispatcherIo"))
      )
   }

   Alog.i(tag, "single    -> ICarRepository")
   single<ICarRepository> { CarRepository(_dao = get<ICarDao>()) }

   Alog.i(tag, "single    -> ITDriveReadModel")
   single<ITDriveReadModel> {
      TDriveReadModel(
         _tDriveDao = get<ITDriveDao>(),
         _dispatcherIo = get<CoroutineDispatcher>(named("dispatcherIo"))
      )
   }
   Alog.i(tag, "single    -> ITDriveRepository")
   single<ITDriveRepository> { TDriveRepository(get()) }

   Alog.i(tag, "single    -> PersonValidator")
   single { PersonValidator(context = androidContext()) }
   Alog.i(tag, "single    -> CarValidator")
   single { CarValidator(context = androidContext()) }
   Alog.i(tag, "single    -> TDriveValidator")
   single { TDriveValidator(context = androidContext()) }

   // Stateless use cases: Koin creates them.
   Alog.i(tag, "factory   -> PersonUcDelete")
   factory {
      PersonUcDelete(
         _repository = get<IPersonRepository>(),
         _imageFileStorage = get<IImageFileStorage>()
      )
   }
   Alog.i(tag, "factory   -> CarUcDelete")
   factory {
      CarUcDelete(
         _repository = get<ICarRepository>(),
         _imageFileStorage = get<IImageFileStorage>()
      )
   }
   Alog.i(tag, "factory   -> TDriveUcDelete")
   factory { TDriveUcDelete(_repository = get()) }


   /*
   IImageEdit ist als factory registriert. Jeder Aufruf von get<IImageEdit>() liefert eine neue
   Instanz. Würde Koin PersonUcCreate selbst erzeugen, bekäme der Use Case seine eigene ImageEdit-
   Instanz, und der ViewModel hätte eine andere. Der ViewModel startet die Sitzung (start, replace),
    der Use Case ruft commit() auf, aber auf einem Objekt, das von den Bildern nichts weiß.
    Es würde nichts gelöscht, und alte Profilbilder blieben liegen.

   Deshalb holt der ViewModel-Block eine ImageEdit-Instanz und gibt sie von Hand an den ViewModel
   und beide Use Cases weiter. Für PersonUcCreate und PersonUcUpdate ist die manuelle Erzeugung
   also Absicht.
    */
   viewModel { parameters ->
      // Both person save use cases must share the ViewModel's image edit session.
      val imageEdit = get<IImageEdit>()

      PersonViewModel(
         personId = parameters.getOrNull<String>(),
         _repository = get<IPersonRepository>(),
         _carReadModel = get<ICarReadModel>(),
         _stringProvider = get<IStringProvider>(),
         _validator = get<PersonValidator>(),
         _imageFileStorage = get<IImageFileStorage>(),
         _imageEdit = imageEdit,
         _personUcCreate = PersonUcCreate(get<IPersonRepository>(), imageEdit),
         _personUcUpdate = PersonUcUpdate(get<IPersonRepository>(), imageEdit),
         _timing = get<AppTiming>(),
         _effectDelegate =
            get<EffectDelegate<PersonEffect>>(personEffectQualifier),
      )
   }

   viewModel {
      val repository = get<IPersonRepository>()

      PeopleViewModel(
         _repository = repository,
         _readmodel = get<IPersonReadModel>(),
         _personUcDelete = PersonUcDelete(
            _repository = repository,
            _imageFileStorage = get<IImageFileStorage>(),
         ),
         _stringProvider = get<IStringProvider>(),
         _timing = get<AppTiming>(),
         _effectDelegate = get<EffectDelegate<PeopleEffect>>(peopleEffectQualifier),
      )
   }

   viewModel { parameters ->
      // IImageEdit is a factory: fetch it once so that the ViewModel and both
      // save use cases share the same image edit session.
      val imageEdit = get<IImageEdit>()

      CarViewModel(
         carId = parameters.getOrNull<String>(),
         _carRepository = get<ICarRepository>(),
         _personReadModel = get<IPersonReadModel>(),
         _stringProvider = get(),
         _validator = get<CarValidator>(),
         _images = ImageEditDelegate(
            session = imageEdit, storage = get<IImageFileStorage>(),
            mode = ImageSelectionMode.APPEND, maxImages = MAX_CAR_IMAGE_COUNT,
         ),
         _carUcCreate = CarUcCreate(get(), imageEdit),
         _carUcUpdate = CarUcUpdate(get(), imageEdit),
         _timing = get<AppTiming>(),
         _effectDelegate =
            get<EffectDelegate<CarEffect>>(carEffectQualifier),
      )
   }
   viewModel {
      CarsViewModel(
         _readModel = get<ICarReadModel>(),
         _carUcDelete = get<CarUcDelete>(),
         _stringProvider = get<IStringProvider>(),
         _timing = get<AppTiming>(),
         _effectDelegate = get<EffectDelegate<CarsEffect>>(carsEffectQualifier),
      )
   }

   viewModel { parameters ->
      TDriveViewModel(
         tDriveId = parameters.getOrNull<String>(),
         _tDriveRepository = get<ITDriveRepository>(),
         _personReadModel = get<IPersonReadModel>(),
         _carReadModel = get<ICarReadModel>(),
         _stringProvider = get<IStringProvider>(),
         _validator = get<TDriveValidator>(),
         _timing = get<AppTiming>(),
         _effectDelegate = get<EffectDelegate<TDriveEffect>>(tDriveEffectQualifier),
      )
   }

   viewModel {
      TDrivesViewModel(
         _readModel = get< ITDriveReadModel>(),
         _tDriveUcDelete = get<TDriveUcDelete>(),
         _stringProvider = get<IStringProvider>(),
         _timing = get<AppTiming>(),
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
 * - PersonUcCreate und PersonUcUpdate teilen dieselbe IImageEdit-Instanz mit
 *   PersonViewModel. PersonUcDelete koordiniert das Room-Repository mit der
 *   Bilddateiverwaltung und respektiert dabei relationale Löschfehler.
 *
 * - Jeder Feature-Bereich besitzt einen eigenen EffectDelegate. Ein globales
 *   CoordinatorViewModel ist weder für Navigation noch für Meldungen notwendig.
 *
 * Lernziele:
 *
 * - Zustandsbehaftete Abhängigkeiten im Composition Root bewusst teilen.
 * - Use Cases für zusammengesetzte Personenoperationen einsetzen.
 * - Relationale Room-Regeln und Dateiverwaltung gemeinsam berücksichtigen.
 */
