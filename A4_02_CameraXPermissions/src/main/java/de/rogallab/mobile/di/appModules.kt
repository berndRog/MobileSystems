package de.rogallab.mobile.di

import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.IImageMediaStore
import de.rogallab.mobile.shared.domain.images.IVideoMediaStore
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.camerax.CameraEffect
import de.rogallab.mobile.ui.camerax.CameraViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun appModule(): Module = module {

   val tag = "<-appModule"

   Alog.i(tag, "factory -> EffectDelegate<CameraEffect>")
   val cameraEffectQualifier = named("CameraEffect")
   factory<EffectDelegate<CameraEffect>>(cameraEffectQualifier) { EffectDelegate() }

   Alog.i(tag, "viewModel -> PersonViewModel")
   viewModel {
      CameraViewModel(
         application = androidApplication(),
         _imageFileStorage = get<IImageFileStorage>(),
         _imageMediaStore = get<IImageMediaStore>(),
         _videoMediaStore = get<IVideoMediaStore>(),
         _stringProvider = get<IStringProvider>(),
         _effectDelegate = get<EffectDelegate<CameraEffect>>(cameraEffectQualifier),
      )
   }
}
