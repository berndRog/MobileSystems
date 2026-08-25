package de.rogallab.mobile.shared.di

import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun utilitiesModule(): Module = module {
   val tag = "<-utilitiesModule"

   Alog.i(tag,"single    -> StringProvider: IStringProvider")
   single<IStringProvider> {
      StringProvider(androidContext())
   }

}