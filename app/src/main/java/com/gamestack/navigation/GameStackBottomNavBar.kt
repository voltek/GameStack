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
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.gamestack.R

private data class BottomNavItem(
    val destination: Destination,
    val labelRes: Int,
    val icon: ImageVector
)

// Each item targets its tab's *graph*, not the screen inside it — that's the
// unit Navigation saves and restores per tab. `selected` below then matches via
// `hierarchy`, so a Detail opened from a tab keeps that tab highlighted.
private val BottomNavItems = listOf(
    BottomNavItem(Destination.HomeGraph, R.string.nav_home, Icons.Filled.Home),
    BottomNavItem(Destination.SearchGraph, R.string.nav_search, Icons.Filled.Search),
    BottomNavItem(Destination.LibraryGraph, R.string.nav_library, Icons.Filled.VideoLibrary)
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
                onClick = { navController.navigateToBottomNavDestination(item.destination) },
                icon = { Icon(imageVector = item.icon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
