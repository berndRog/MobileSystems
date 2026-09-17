package de.rogallab.mobile.di

import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.remote.Seed
import de.rogallab.mobile.data.remote.SeedApi
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.data.network.NetworkExceptionMapper
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit

fun appModule(): Module = module {

   val tag = "<-appModule"

   Alog.i(tag, "single    -> IPersonWebservice")
   single<IPersonWebservice> {
      get<Retrofit>().create(IPersonWebservice::class.java)
   }

   Alog.i(tag, "single    -> Seed")
   single<Seed> {
      Seed(
         _imageFileStorage = get<IImageFileStorage>(),
      )
   }

   Alog.i(tag, "single    -> SeedApi")
   single<SeedApi> {
      SeedApi(
         _personWeb = get<IPersonWebservice>(),
         _seed = get<Seed>(),
      )
   }

   Alog.i(tag, "single    -> PersonRepository: IPersonRepository")
   single<IPersonRepository> {
      PersonRepository(
         _webservice = get<IPersonWebservice>(),
         _networkExceptionMapper = get<NetworkExceptionMapper>(),
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
 * - A5_11_PeopleImagesRetrofit behält denselben Repository-Port wie A5_10.
 *   Personendaten verwenden weiterhin den bereits bekannten JSON-Vertrag.
 *
 * - Die generische Netzwerk-Infrastruktur mit OkHttp, Json und Retrofit liegt in
 *   Shared. Dieses Modul registriert nur die projektspezifische Schnittstelle
 *   IPersonWebservice und erzeugt daraus den Client für PeopleImagesApi.
 *   Auch der NetworkExceptionMapper kommt aus Shared und wird dem
 *   projektspezifischen Repository injiziert.
 *
 * - Seed und SeedApi demonstrieren zusätzlich eine Initialisierung über REST:
 *   countAll() prüft, ob der Server leer ist. Danach werden Personendaten als
 *   JSON und Bilddateien jeweils über den getrennten Multipart-Endpunkt gesendet.
 *
 * - Die Abhängigkeitskette lautet damit:
 *
 *      PeopleViewModel / PersonViewModel
 *          -> IPersonRepository
 *          -> PersonRepository
 *          -> IPersonWebservice
 *          -> Retrofit
 *          -> OkHttp
 *          -> PeopleImagesApi
 *
 * - IImageFileStorage verwaltet lokale Galerie-, Kamera- und Seed-Dateien nur bis
 *   zum erfolgreichen Upload. Persistente Bilder und deren URLs gehören danach
 *   vollständig PeopleImagesApi.
 */
