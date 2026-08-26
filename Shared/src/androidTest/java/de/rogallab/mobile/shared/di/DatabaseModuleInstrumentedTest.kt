package de.rogallab.mobile.shared.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.shared.data.IPersonDao
import de.rogallab.mobile.shared.data.local.database.AppDatabasePerson
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication

@RunWith(AndroidJUnit4::class)
class DatabaseModuleInstrumentedTest {

   @Test
   fun databaseModule_providesDatabaseAndDaoAsSingletons() {
      val context =
         ApplicationProvider.getApplicationContext<Context>()
      val databaseName =
         "shared-module-test-${UUID.randomUUID()}.db"

      val koin = koinApplication {
         androidContext(context)
         modules(
            databaseModule(
               databaseName = databaseName,
               ioDispatcher = Dispatchers.IO,
            )
         )
      }.koin

      val database1 = koin.get<AppDatabasePerson>()
      val database2 = koin.get<AppDatabasePerson>()
      val dao1 = koin.get<IPersonDao>()
      val dao2 = koin.get<IPersonDao>()

      assertSame(database1, database2)
      assertSame(dao1, dao2)

      database1.close()
      context.deleteDatabase(databaseName)
   }
}
