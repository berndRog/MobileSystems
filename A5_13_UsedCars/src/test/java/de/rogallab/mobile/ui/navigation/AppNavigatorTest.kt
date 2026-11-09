package de.rogallab.mobile.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.domain.utilities.AppLogger
import de.rogallab.mobile.ui.navigation.comp.AppNavigationState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AppNavigatorTest {
   @Before fun setUp() = AppLogger.disable()
   @After fun tearDown() = AppLogger.reset()

   @Test fun backFromCarsAndTDrivesRootsReturnsToPeopleWithoutClearingTheirStacks() {
      val people = NavBackStack<NavKey>(PersonListKey)
      val cars = NavBackStack<NavKey>(CarListKey)
      val tDrives = NavBackStack<NavKey>(TDrivesKey)
      val state = AppNavigationState(
         PeopleTopLevel,
         topLevelNavItems,
         mutableStateOf(0),
         mapOf(PeopleTopLevel to people, CarsTopLevel to cars, TDrivesTopLevel to tDrives),
      )
      val navigator = AppNavigator(state)

      navigator.switchTopLevel(CarsTopLevel)
      navigator.push(CarKey("car-1"))
      navigator.pop()
      assertEquals(CarsTopLevel, state.currentTopLevelNavItem)
      assertEquals(listOf(CarListKey), cars.toList())
      navigator.pop()
      assertEquals(PeopleTopLevel, state.currentTopLevelNavItem)
      assertEquals(listOf(CarListKey), cars.toList())

      navigator.switchTopLevel(TDrivesTopLevel)
      navigator.push(TDriveKey("drive-1"))
      navigator.pop()
      assertEquals(TDrivesTopLevel, state.currentTopLevelNavItem)
      assertEquals(listOf(TDrivesKey), tDrives.toList())
      navigator.pop()
      assertEquals(PeopleTopLevel, state.currentTopLevelNavItem)
      assertEquals(listOf(TDrivesKey), tDrives.toList())
      navigator.pop()
      assertEquals(listOf(PersonListKey), people.toList())
   }
}
