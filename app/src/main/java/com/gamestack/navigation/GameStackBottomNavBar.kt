package com.gamestack.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gamestack.R

private data class BottomNavItem(
    val destination: Destination,
    val labelRes: Int,
    val icon: ImageVector
)

private val BottomNavItems = listOf(
    BottomNavItem(Destination.Home, R.string.nav_home, Icons.Filled.Home),
    BottomNavItem(Destination.Search, R.string.nav_search, Icons.Filled.Search),
    BottomNavItem(Destination.Library, R.string.nav_library, Icons.Filled.VideoLibrary)
)

@Composable
fun GameStackBottomNavBar(navController: NavHostController) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    NavigationBar {
        BottomNavItems.forEach { item ->
            val label = stringResource(item.labelRes)
            val selected =
                currentDestination?.hierarchy?.any { it.hasRoute(item.destination::class) } ?: false

            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.destination) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(imageVector = item.icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
