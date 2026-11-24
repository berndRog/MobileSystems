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
      val people = NavBackStack<NavKey>(PeopleKey)
      val cars = NavBackStack<NavKey>(CarsKey)
      val tDrives = NavBackStack<NavKey>(TDrivesKey)
      val state = AppNavigationState(
         PeopleTopLevel,
         topLevelNavItems,
         mutableStateOf(0),
         mapOf(PeopleTopLevel to people, CarsTopLevel to cars, TDrivesTopLevel to tDrives),
      )
      val navigator = AppNavigator(state)

      navigator.switchTopLevel(CarsTopLevel)
      navigator.add(CarKey("car-1"))
      navigator.remove()
      assertEquals(CarsTopLevel, state.currentTopLevelNavItem)
      assertEquals(listOf(CarsKey), cars.toList())
      navigator.remove()
      assertEquals(PeopleTopLevel, state.currentTopLevelNavItem)
      assertEquals(listOf(CarsKey), cars.toList())

      navigator.switchTopLevel(TDrivesTopLevel)
      navigator.add(TDriveKey("drive-1"))
      navigator.remove()
      assertEquals(TDrivesTopLevel, state.currentTopLevelNavItem)
      assertEquals(listOf(TDrivesKey), tDrives.toList())
      navigator.remove()
      assertEquals(PeopleTopLevel, state.currentTopLevelNavItem)
      assertEquals(listOf(TDrivesKey), tDrives.toList())
      navigator.remove()
      assertEquals(listOf(PeopleKey), people.toList())
   }
}
