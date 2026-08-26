package de.rogallab.mobile.shared.di

import de.rogallab.mobile.shared.data.local.io.ImageFileStorage
import de.rogallab.mobile.shared.data.local.io.ImageMediaStore
import de.rogallab.mobile.shared.domain.io.IImageFileStorage
import de.rogallab.mobile.shared.domain.io.IImageMediaStore
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.images.IImageEdit
import de.rogallab.mobile.shared.ui.images.ImageEditDelegate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Creates the optional Koin module for the reusable image services.
 */
fun imageStorageModule(
   directoryName: String,
   ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) = module {

   val tag = "<-imageStorageModule"

   Alog.i(tag,"single    -> ImageFileStorage: IImageFileStorage,  directory=$directoryName")
   single<IImageFileStorage> {
      ImageFileStorage(
         context = androidContext(),
         directoryName = directoryName,
         ioDispatcher = ioDispatcher,
      )
   }

   Alog.i(tag,"single    -> ImageMediaStore: IImageMediaStore")
   single<IImageMediaStore> {
      ImageMediaStore(
         context = androidContext(),
         ioDispatcher = ioDispatcher,
      )
   }

   Alog.i(tag,"factory   -> ImageEditDelegate: IImageEdit")
   factory<IImageEdit> {
      ImageEditDelegate(
         _imageFileStorage = get<IImageFileStorage>(),
      )
   }

}