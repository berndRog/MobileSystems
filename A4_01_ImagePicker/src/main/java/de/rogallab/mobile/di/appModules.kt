package de.rogallab.mobile.di

import de.rogallab.mobile.data.local.Seed
import de.rogallab.mobile.data.local.SeedDatabase
import de.rogallab.mobile.data.repositories.PersonRepository
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.usecases.PersonUcCreate
import de.rogallab.mobile.domain.usecases.PersonUcDelete
import de.rogallab.mobile.domain.usecases.PersonUcUpdate
import de.rogallab.mobile.shared.data.local.IPersonDao
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
        // Both save use cases must share the ViewModel's image edit session.
        val repository = get<IPersonRepository>()
        val imageEdit = get<IImageEdit>()

        PersonViewModel(
           personId = parameters.getOrNull<String>(),
           _repository = repository,
           _stringProvider = get(),
           _validator = get<PersonValidator>(),
           _imageFileStorage = get<IImageFileStorage>(),
           _imageEdit = imageEdit,
           _personUcCreate = PersonUcCreate(repository, imageEdit),
           _personUcUpdate = PersonUcUpdate(repository, imageEdit),
           _effectDelegate = get<EffectDelegate<PersonEffect>>(personEffectQualifier),
        )
    }

    Alog.i(tag, "viewModel -> PeopleViewModel")
    viewModel {
        val repository = get<IPersonRepository>()

        PeopleViewModel(
           _repository = repository,
           _personUcDelete = PersonUcDelete(
              _repository = repository,
              _imageFileStorage = get<IImageFileStorage>(),
           ),
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
 * - PersonUcCreate und PersonUcUpdate verwenden dieselbe
 *   IImageEdit-Instanz wie PersonViewModel. Nur so schließen sie genau die
 *   Bild-Session ab, die das ViewModel während der Bearbeitung aufgebaut hat.
 *
 * - PersonUcDelete kombiniert das Repository mit IImageFileStorage, weil
 *   das Löschen ab A4_01 sowohl den Datensatz als auch die Bilddatei betrifft.
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
 * - Zustandsbehaftete Abhängigkeiten im Composition Root bewusst teilen.
 * - Use Cases nur für zusammengesetzte Anwendungsoperationen bereitstellen.
 */
