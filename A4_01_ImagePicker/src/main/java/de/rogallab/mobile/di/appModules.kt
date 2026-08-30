package de.rogallab.mobile.di

import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.SeedDatabase
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.shared.data.IPersonDao
import de.rogallab.mobile.shared.data.local.database.AppDatabasePerson
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.shared.ui.images.IImageEdit
import de.rogallab.mobile.ui.people.PersonValidator
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun appModule(): Module = module {

    val tag = "<-appModule"

    Alog.i(tag, "single    -> Seed")
    single<Seed> {
        Seed(
           _imageFileStorage = get<IImageFileStorage>()
        )
    }

    Alog.i(tag, "single    -> SeedDatabase")
    single<SeedDatabase> {
        SeedDatabase(
           _personDao = get<IPersonDao>(),
           _database = get<AppDatabasePerson>(),
           _seed = get<Seed>()
        )
    }

    Alog.i(tag, "single    -> PersonRepository: IPersonRepository")
    single<IPersonRepository> {
        PersonRepository(
           _personDao = get<IPersonDao>()
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
           _effectDelegate = get<EffectDelegate<PersonEffect>>(personEffectQualifier),
        )
    }

    Alog.i(tag, "viewModel -> PeopleViewModel")
    viewModel {
        PeopleViewModel(
           _repository = get<IPersonRepository>(),
           _stringProvider = get(),
           _effectDelegate = get<EffectDelegate<PeopleEffect>>(peopleEffectQualifier),
        )
    }

}

/*
 * Didaktik und Lernziele
 *
 * - A4_01 konzentriert sich auf den ImagePicker und benötigt für Swipe-to-Delete
 *   keinen zusätzlichen zustandsbehafteten VisualRemovalDelegate.
 *
 * - PeopleViewModel erhält deshalb nur Repository, StringProvider und
 *   EffectDelegate. Die Löschbestätigung wird vollständig über Intent und Effect
 *   modelliert; vor der Bestätigung wird kein eigener Removal-State aufgebaut.
 *
 * - PersonViewModel erhält weiterhin IImageEdit und IImageFileStorage. Damit
 *   bleibt die Delegation des Bild-Lebenszyklus das neue Architekturthema dieses
 *   Schritts.
 *
 * - A4_02_ImagePickerUndo ergänzt später wieder IVisualRemoval<Person> als
 *   zustandsbehaftete Abhängigkeit und macht die zusätzliche Komplexität damit
 *   ausdrücklich sichtbar.
 *
 * Lernziele:
 *
 * - Nur tatsächlich benötigte Abhängigkeiten per Constructor Injection liefern.
 * - Den ImagePicker unabhängig von der Undo-Infrastruktur behandeln.
 * - Einfache Löschbestätigung und zustandsbehaftetes Undo vergleichen können.
 */
