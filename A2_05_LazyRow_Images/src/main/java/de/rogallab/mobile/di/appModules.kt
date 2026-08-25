package de.rogallab.mobile.di

import de.rogallab.mobile.data.ImageRepository
import de.rogallab.mobile.data.Seed
import de.rogallab.mobile.domain.IImageRepository
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.features.images.ImageViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val defineModules: Module = module {
   val tag = "<-dataModules"

   AppLogger.info(tag, "single    -> Seed")
   single<Seed> {
      Seed(_context = androidContext())  // dependency injection of Android context
   }

   AppLogger.info(tag, "single    -> PersonRepository: IPersonRepository")
   single<IImageRepository> {
      ImageRepository(
         _seed = get<Seed>()  // dependency injection of DataStore
      )
   }

   AppLogger.info(tag, "viewModel -> PersonViewModel")
   viewModel {
      ImageViewModel(
         _repository = get<IImageRepository>(),
      )
   }
}

val appModules: Module = module {
   try {
      val testedModules = defineModules
      requireNotNull(testedModules) {
         "defineModules failed"
      }
      includes(
         testedModules,
         //testedUiModules,
         //useCaseModules
      )
   } catch (e: Exception) {
      AppLogger.info("<-appModules", e.message!!)
   }
}