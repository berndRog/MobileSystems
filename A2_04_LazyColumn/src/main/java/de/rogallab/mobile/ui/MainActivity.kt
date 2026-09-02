package de.rogallab.mobile.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.rogallab.mobile.domain.entities.Person
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


@Composable
fun TestLazyColumn(
   modifier: Modifier = Modifier.Companion
) {

   val people = remember {
      mutableStateListOf(
         Person("Arne", "Arndt"),
         Person("Berta", "Bauer"),
         Person("Cord", "Conrad"),
         Person("Dagmar", "Diehl")
      )
   }

   LazyColumn(
      state = rememberLazyListState(),
      modifier = modifier
   ) {
      // Single item
      item {
         HorizontalDivider()
         Text(
            text = "Single Item",
            modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth()
         )
         HorizontalDivider()
      }

      // multiple items addressed by index
      items(
         count = people.size,
      ) { index ->
         val person = people[index]
         Column(modifier = Modifier.fillMaxWidth()) {
            Text(
               text = "$index",
               modifier = Modifier.padding(top = 8.dp)
            )
            Text(
               text = "${person.firstName} ${person.lastName}",
               modifier = Modifier.padding(bottom = 8.dp))
            HorizontalDivider()
         }

      }

      // multiple items with key
      items(
         items = people,
         key = { person -> person.id }
      ) { person ->
         Column(modifier = Modifier.fillMaxWidth()) {
            Text(
               text = person.id,
               modifier = Modifier.padding(top = 8.dp)
            )
            Text(
               text = "${person.firstName} ${person.lastName}",
               modifier = Modifier.padding(bottom = 8.dp))
            HorizontalDivider()
         }
      }
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