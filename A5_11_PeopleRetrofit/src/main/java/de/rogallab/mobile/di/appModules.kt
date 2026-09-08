package de.rogallab.mobile.di

import de.rogallab.mobile.BuildConfig
import de.rogallab.mobile.Globals
import de.rogallab.mobile.data.remote.IPersonWebservice
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

fun appModule(): Module = module {

   val tag = "<-appModule"

   Alog.i(tag, "single    -> HttpLoggingInterceptor")
   single<HttpLoggingInterceptor> {
      HttpLoggingInterceptor().apply {
         level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BASIC
         }
         else {
            HttpLoggingInterceptor.Level.NONE
         }
      }
   }

   Alog.i(tag, "single    -> OkHttpClient")
   single<OkHttpClient> {
      OkHttpClient.Builder()
         .addInterceptor(get<HttpLoggingInterceptor>())
         .build()
   }

   Alog.i(tag, "single    -> Retrofit")
   single<Retrofit> {
      Retrofit.Builder()
         .baseUrl(Globals.baseUrl)
         .client(get<OkHttpClient>())
         .addConverterFactory(GsonConverterFactory.create())
         .build()
   }

   Alog.i(tag, "single    -> IPersonWebservice")
   single<IPersonWebservice> {
      get<Retrofit>().create(IPersonWebservice::class.java)
   }

   Alog.i(tag, "single    -> PersonRepository: IPersonRepository")
   single<IPersonRepository> {
      PersonRepository(
         _webservice = get<IPersonWebservice>(),
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
 * - A5_11_PeopleRetrofit behält dieselben ViewModels und denselben Repository-Port
 *   wie A5_01. Geändert wird ausschließlich die Data-/Infrastructure-Seite.
 *
 * - OkHttp führt HTTP aus, Retrofit beschreibt die Webservice-Aufrufe und Gson
 *   übersetzt JSON in PersonDto. PersonRepository bildet diese DTOs anschließend
 *   auf die Domain-Entität Person ab.
 *
 * - Die Abhängigkeitskette lautet damit:
 *
 *      PeopleViewModel / PersonViewModel
 *          -> IPersonRepository
 *          -> PersonRepository
 *          -> IPersonWebservice
 *          -> Retrofit
 *          -> OkHttp
 *          -> PeopleApi
 *
 * - IImageFileStorage bleibt Shared-Infrastruktur, wird jetzt aber nur für lokale
 *   temporäre Galerie-/Kamera-Dateien benötigt. Persistente Bilder gehören dem
 *   Server und werden nicht durch IImageEdit im Android-Client verwaltet.
 *
 * Lernziele:
 *
 * - Retrofit und OkHttp per Koin bereitstellen.
 * - Repository-Port beim Wechsel von Room zu REST unverändert weiterverwenden.
 * - HTTP-/DTO-Technik von ViewModel und Domain-Modell trennen.
 */
