package de.rogallab.mobile.shared.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.IImageMediaStore
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.images.IImageEdit
import org.junit.After
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.koinApplication
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(
   sdk = [35],
   application = Application::class,
)
class SharedModulesTest {

   private val context =
      ApplicationProvider.getApplicationContext<Application>()

   @After
   fun tearDown() {
      Alog.reset()
   }

   @Test
   fun utilitiesModule_providesStringProviderAsSingleton() {
      Alog.set(useAndroidLog = false)

      val koin = koinApplication {
         androidContext(context)
         modules(utilitiesModule())
      }.koin

      val first = koin.get<IStringProvider>()
      val second = koin.get<IStringProvider>()

      assertSame(first, second)
   }

   @Test
   fun imageStorageModule_providesStorageServicesAsSingletons() {
      Alog.set(useAndroidLog = false)

      val koin = koinApplication {
         androidContext(context)
         modules(imageStorageModule("module-test-images"))
      }.koin

      assertSame(
         koin.get<IImageFileStorage>(),
         koin.get<IImageFileStorage>()
      )
      assertSame(
         koin.get<IImageMediaStore>(),
         koin.get<IImageMediaStore>()
      )
   }

   @Test
   fun imageStorageModule_providesImageEditAsFactory() {
      Alog.set(useAndroidLog = false)

      val koin = koinApplication {
         androidContext(context)
         modules(imageStorageModule("module-test-images"))
      }.koin

      val first = koin.get<IImageEdit>()
      val second = koin.get<IImageEdit>()

      assertNotSame(first, second)
   }
}
