package de.rogallab.mobile.ui.people

import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.rogallab.mobile.AppTiming
import de.rogallab.mobile.R
import de.rogallab.mobile.domain.IPersonRepository
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.shared.data.network.ServerUnreachableException
import de.rogallab.mobile.shared.domain.utilities.StringProvider
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.people.list.PeopleEffect
import de.rogallab.mobile.ui.people.list.PeopleIntent
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeopleEffectIntegrationTest {
   @Test fun navigationResourceAndNetworkMessages_useTheSameEffectSource() = runBlocking {
      val context = ApplicationProvider.getApplicationContext<android.content.Context>()
      val source = MutableStateFlow<Result<List<Person>>>(Result.success(emptyList()))
      // No live API is needed to exercise the Android scope and real resources.
      val repository = object : IPersonRepository {
         override fun observeAll(): Flow<Result<List<Person>>> = source
         override suspend fun findById(id: String): Result<Person?> = Result.success(null)
         override suspend fun create(person: Person): Result<Unit> = Result.success(Unit)
         override suspend fun update(person: Person): Result<Unit> = Result.success(Unit)
         override suspend fun remove(person: Person): Result<Unit> = Result.success(Unit)
      }
      val store = ViewModelStore()
      try {
         val vm = withContext(Dispatchers.Main) {
            PeopleViewModel(repository, StringProvider(context), EffectDelegate(),
               AppTiming(delayRead = 0, delayWrite = 0)).also {
               store.put("people", it)
               it.onIntent(PeopleIntent.Detail("p1"))
            }
         }
         withTimeout(5_000) {
            assertEquals(PeopleEffect.NavigateTo("p1"), vm.effects.first())
            withContext(Dispatchers.Main) { vm.onIntent(PeopleIntent.RequestRemove("missing")) }
            assertEquals(PeopleEffect.ShowError(context.getString(R.string.error_person_not_found)), vm.effects.first())
            source.value = Result.failure(ServerUnreachableException("Controlled network error"))
            assertEquals(PeopleEffect.ShowError("Controlled network error"), vm.effects.first())
         }
      } finally {
         withContext(Dispatchers.Main) { store.clear() }
      }
   }
}
