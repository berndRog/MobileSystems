package de.rogallab.mobile

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.data.local.SeedDatabase
import de.rogallab.mobile.di.appModule
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.di.databaseModule
import de.rogallab.mobile.shared.domain.utilities.Alog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.get
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {

   private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

   override fun onCreate() {
      super.onCreate()

      Alog.set(
         useAndroidLog = true,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = true
      )

      // Initialize any global state or dependencies here
      // Composition Root for Koin
      startKoin {
         androidLogger(Level.DEBUG)
         androidContext(androidContext = this@MainApplication)
         modules(appModule())
         modules(databaseModule(Globals.databaseName))
      }

      val seedDatabase: SeedDatabase = get()
      val job = appScope.launch {
         seedDatabase.seedPerson()
      }
      appScope.launch {
         job.join()   // waits until seeding is finished
      }

   }

   companion object {
      private const val TAG = "<-MainApplication"
   }
}