package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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

   // Activity-scoped ViewModels viewModelStoreOwner = MainActivity
   // initialize ViewModel here when needed in Activity / setContent
   // lazy initialization of the ViewModel with koin
   // private val _personViewModel: PersonViewModel by viewModel()

   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)

      enableEdgeToEdge()

      setContent {

         AppTheme {
            Scaffold(
               modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
//               TestLazyColumn(
//                  modifier = Modifier
//                     .padding(innerPadding)
//                     .padding(top = 8.dp)
//                     .fillMaxSize()
//               )

//               // InputScreen, i.e. create a new person
//               val personViewModel = koinViewModel<PersonViewModel> {
//                  parametersOf(null)
//               }
//               PersonAdapter(
//                  viewModel = personViewModel,
//                  modifier = Modifier
//                     .padding(innerPadding)
//               )

               val peopleViewModel = koinViewModel<PeopleViewModel>()
               PeopleAdapter(
                  viewModel = peopleViewModel,
                  modifier = Modifier
                     .padding(innerPadding).padding(horizontal = 16.dp)
                     .fillMaxSize()
               )
            }
         }
      }
   }

   companion object {
      private const val TAG = "<-MainActivity"
   }
}

@Preview(showBackground = true)
@Composable
fun Preview() {

   val viewModel = koinViewModel<PersonViewModel>()

   AppTheme {
      Scaffold(
         contentWindowInsets = WindowInsets.safeDrawing,
         modifier = Modifier
            .padding(top = 8.dp)
            .padding(horizontal = 16.dp)
            .fillMaxSize()
      ) { innerPadding ->
//       LazyColumnTest()
//         TaskItem1(
//            id = 1,                            // State ↓
//            label = "Task",                      // State ↓
//            onClose = {},              // Event ↑  IconButton
//            onClicked = {}            // Event ↑  TaskItem
//         )

         // InputScreen, i.e. create a new person
         val viewModel = koinViewModel<PersonViewModel>()
         val personViewModel = org.koin.compose.viewmodel.koinViewModel<PersonViewModel> {
            parametersOf(null)
         }
         PersonAdapter(
            viewModel = personViewModel,
            modifier = Modifier
               .padding(innerPadding)
         )

//         PeopleAdapter(
//            modifier = Modifier
//               .padding(innerPadding)
//         )
      }
   }
}