package de.rogallab.mobile.shared.di

import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import de.rogallab.mobile.shared.data.IPersonDao
import de.rogallab.mobile.shared.data.local.database.AppDatabasePerson
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

fun databaseModule(
   databaseName: String,
   ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
): Module = module {

   val tag = "<-databaseModule"

   Alog.i(tag,"single    -> AppDatabasePerson,  name=$databaseName")
   single<AppDatabasePerson> {
      Room.databaseBuilder<AppDatabasePerson>(
         context = androidContext(),
         name = databaseName,
      )
         .setDriver(BundledSQLiteDriver())
         .setQueryCoroutineContext(ioDispatcher)
         .build()
   }

   Alog.i(tag,"single    -> generated: IPersonDao")
   single<IPersonDao> {
      get<AppDatabasePerson>().createPersonDao()
   }

}