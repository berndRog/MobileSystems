package de.rogallab.mobile.ui.people.create_detail

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.rogallab.mobile.shared.domain.utilities.Alog

@Composable
fun PersonAdapterSth(
   modifier: Modifier
) {
   val tag = "<-PersonAdapterSth"
   val nComp = remember { mutableIntStateOf(1) }
   SideEffect { Alog.c(tag, "Composition #${nComp.value++}") }

   // Observable local state,  state by delegate
   var firstName by rememberSaveable { mutableStateOf("") }
   var lastName  by rememberSaveable { mutableStateOf("") }

   PersonScreen(
      firstName = firstName,                     // State ↓
      onFirstNameChange = { it: String ->        // Event ↑
         Alog.d(tag, "onFirstNameChange: $it")
         firstName = it
      },

      lastName = lastName,
      onLastNameChange = { it: String ->
         Alog.d(tag, "onLastNameChange: $it")
         lastName = it
      },
      onSave = {
         Alog.d(tag, "onSave clicked")
      },
      onCancel = {
         Alog.d(tag, "onCancel clicked")
      },
      modifier = modifier
   )

}