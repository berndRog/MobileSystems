package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.shared.ui.BaseActivity
import de.rogallab.mobile.ui.people.create_detail.PersonViewModel
import de.rogallab.mobile.ui.people.create_detail.comp.PersonAdapter
import de.rogallab.mobile.ui.people.list.PeopleViewModel
import de.rogallab.mobile.ui.people.list.comp.PeopleAdapter
import de.rogallab.mobile.ui.theme.AppTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

class MainActivity : BaseActivity(TAG) {

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()

      setContent {

         // DetailScreen
         val personViewModel = koinViewModel<PersonViewModel> {
            parametersOf(null) // New Person
            //parametersOf("01000000-0000-0000-0000-000000000000")
         }

         // ListScreen
         val peopleViewModel = koinViewModel<PeopleViewModel>()

         AppTheme {
//            PersonAdapter(
//               viewModel = personViewModel
//            )

            PeopleAdapter(
               viewModel = peopleViewModel
            )

         }
      }
   }

   companion object {
      private const val TAG = "<-MainActivity"
   }
}

private fun isInTest(): Boolean {
   return try {
      Class.forName("androidx.test.espresso.Espresso")
      true
   } catch (e: ClassNotFoundException) {
      false
   }
}

