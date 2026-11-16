package de.rogallab.mobile.di

import de.rogallab.mobile.domain.usecases.PhotoUcSave
import de.rogallab.mobile.domain.usecases.VideoUcFinish
import de.rogallab.mobile.shared.domain.IStringProvider
import de.rogallab.mobile.shared.domain.images.IImageEdit
import de.rogallab.mobile.shared.domain.images.IImageFileStorage
import de.rogallab.mobile.shared.domain.images.IImageMediaStore
import de.rogallab.mobile.shared.domain.images.IVideoMediaStore
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.effects.EffectDelegate
import de.rogallab.mobile.ui.camerax.CameraEffect
import de.rogallab.mobile.ui.camerax.CameraViewModel
import de.rogallab.mobile.ui.videoviewer.VideoViewerViewModel
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

   Alog.i(tag, "factory   -> PhotoUcSave")
   factory<PhotoUcSave> {
      PhotoUcSave(
         _imageFileStorage = get<IImageFileStorage>(),
         _imageMediaStore = get<IImageMediaStore>()
      )
   }

   Alog.i(tag, "factory   -> VideoUcFinish")
   factory<VideoUcFinish> {
      VideoUcFinish(
         _videoMediaStore = get<IVideoMediaStore>()
      )
   }


   Alog.i(tag, "viewModel -> CameraViewModel")
   viewModel {
      CameraViewModel(
         application = androidApplication(),
         _imageFileStorage = get<IImageFileStorage>(),
         _videoMediaStore = get<IVideoMediaStore>(),
         _photoUcSave = get<PhotoUcSave>(),
         _videoUcFinish = get<VideoUcFinish>(),
         _stringProvider = get<IStringProvider>(),
         _effectDelegate = get<EffectDelegate<CameraEffect>>(cameraEffectQualifier),
      )
   }
   Alog.i(tag, "viewModel -> VideoViewerViewModel")
   viewModel {
      VideoViewerViewModel()
   }
}
