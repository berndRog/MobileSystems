package de.rogallab.mobile.di

import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.list.PeopleEffect
import org.koin.core.qualifier.named
import org.koin.dsl.module

val personEffectQualifier = named("personEffect")
val peopleEffectQualifier = named("peopleEffect")

/**
 * Provides a separate EffectDelegate for every feature ViewModel instance.
 */
fun effectModule() = module {

   // Creates a new PersonEffect delegate for each PersonViewModel.
   factory<EffectDelegate<PersonEffect>>(personEffectQualifier) {
      EffectDelegate<PersonEffect>()
   }

   // Creates a new PeopleEffect delegate for each PeopleViewModel.
   factory<EffectDelegate<PeopleEffect>>(peopleEffectQualifier) {
      EffectDelegate<PeopleEffect>()
   }
}

/*
 * Didaktik und Lernziele
 *
 * - EffectDelegate ist generisch. Auf der JVM werden die generischen
 *   Typparameter zur Laufzeit jedoch nicht zur Unterscheidung von Koin-
 *   Definitionen verwendet.
 *
 * - Deshalb erhalten PersonEffect und PeopleEffect eindeutige Qualifier.
 *
 * - factory erzeugt für jede ViewModel-Instanz einen eigenen Delegate und
 *   damit einen eigenen Channel für einmalige Effects.
 *
 * Lernziele:
 *
 * - Generische Abhängigkeiten im DI-Container eindeutig registrieren.
 * - factory und Qualifier in Koin einsetzen.
 */
