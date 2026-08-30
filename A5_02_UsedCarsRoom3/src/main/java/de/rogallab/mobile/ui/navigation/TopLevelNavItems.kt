package de.rogallab.mobile.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.People
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import de.rogallab.mobile.R

data object PeopleTopLevel : ITopLevelNavItem {
   override val navKey: NavKey = PersonListKey
   override val labelResourceId: Int = R.string.navigation_people
   override val logLabel: String = "People"
   override val iconActive: ImageVector = Icons.Filled.People
   override val iconOutlined: ImageVector = Icons.Outlined.People
}

data object CarsTopLevel : ITopLevelNavItem {
   override val navKey: NavKey = CarListKey
   override val labelResourceId: Int = R.string.navigation_cars
   override val logLabel: String = "Cars"
   override val iconActive: ImageVector = Icons.Filled.DirectionsCar
   override val iconOutlined: ImageVector = Icons.Outlined.DirectionsCar
}

data object TDrivesTopLevel : ITopLevelNavItem {
   override val navKey: NavKey = TDrivesKey
   override val labelResourceId: Int = R.string.navigation_test_drives
   override val logLabel: String = "TestDrives"
   override val iconActive: ImageVector = Icons.Filled.Event
   override val iconOutlined: ImageVector = Icons.Outlined.Event
}

val topLevelNavItems: List<ITopLevelNavItem> = listOf(
   PeopleTopLevel,
   CarsTopLevel,
   TDrivesTopLevel,
)
