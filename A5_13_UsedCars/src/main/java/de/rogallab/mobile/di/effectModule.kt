package de.rogallab.mobile.di

import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.cars.input_detail.CarEffect
import de.rogallab.mobile.ui.cars.list.CarsEffect
import de.rogallab.mobile.ui.people.create_detail.PersonEffect
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.tdrives.input_detail.TDriveEffect
import de.rogallab.mobile.ui.tdrives.list.TDrivesEffect
import org.koin.core.qualifier.named
import org.koin.dsl.module

val personEffectQualifier = named("personEffect")
val peopleEffectQualifier = named("peopleEffect")
val carEffectQualifier = named("carEffect")
val carsEffectQualifier = named("carsEffect")
val tDriveEffectQualifier = named("tDriveEffect")
val tDrivesEffectQualifier = named("tDrivesEffect")

fun effectModule() = module {
   factory<EffectDelegate<PersonEffect>>(personEffectQualifier) { EffectDelegate() }
   factory<EffectDelegate<PeopleEffect>>(peopleEffectQualifier) { EffectDelegate() }
   factory<EffectDelegate<CarEffect>>(carEffectQualifier) { EffectDelegate() }
   factory<EffectDelegate<CarsEffect>>(carsEffectQualifier) { EffectDelegate() }
   factory<EffectDelegate<TDriveEffect>>(tDriveEffectQualifier) { EffectDelegate() }
   factory<EffectDelegate<TDrivesEffect>>(tDrivesEffectQualifier) { EffectDelegate() }
}
