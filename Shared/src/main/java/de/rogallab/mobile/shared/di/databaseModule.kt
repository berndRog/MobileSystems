package de.rogallab.mobile.shared.di

import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.coroutines.CoroutineContext

inline fun <reified TDatabase : RoomDatabase> databaseModule(
   databaseName: String,
   queryCoroutineContext: CoroutineContext = Dispatchers.IO,
): Module = module {

   single<TDatabase> {
      Room.databaseBuilder<TDatabase>(
         context = androidContext(),
         name = databaseName,
      )
      .setDriver(BundledSQLiteDriver())
      .setQueryCoroutineContext(queryCoroutineContext)
      .build()
   }
}