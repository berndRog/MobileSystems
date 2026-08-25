package de.rogallab.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.domain.utilities.Alog
import de.rogallab.mobile.shared.ui.BaseActivity
import de.rogallab.mobile.ui.people.create_detail.mvvm.comp.PersonAdapterMvvm
import de.rogallab.mobile.ui.theme.AppTheme

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      Alog.set(
         useAndroidLog = true,
         isVerbose = true,
         isDebug = true,
         isInfo = true,
         isComp = false
      )

      enableEdgeToEdge()

      setContent {
         Alog.d("<-ComposeView", "Composition")

         AppTheme {
            Scaffold(
               modifier = Modifier
                  .padding(all = 16.dp).fillMaxSize()
            ) { innerPadding ->

//               PersonAdapterStateHolder(
//                  modifier = Modifier
//                     .padding(innerPadding).fillMaxWidth()
//               )

               PersonAdapterMvvm(
                  modifier = Modifier
                     .padding(innerPadding).fillMaxWidth()
               )

//               PersonAdapterMvi(
//                  modifier = Modifier
//                     .padding(innerPadding).fillMaxWidth()
//               )
            }
         }
      }
   }

   companion object {
      private const val TAG = "<-MainActivity"
   }
}