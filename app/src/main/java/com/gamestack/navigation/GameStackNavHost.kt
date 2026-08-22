package com.gamestack.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.gamestack.R
import com.gamestack.feature.search.presentation.SearchScreen

@Composable
fun GameStackApp() {
    val navController = rememberNavController()
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination

    Scaffold(
        // Detail keeps its own collapsing top bar (future work) — never this static one.
        topBar = {
            if (currentDestination?.hasRoute(Destination.Detail::class) != true) {
                GameStackTopAppBar()
            }
        },
        bottomBar = { GameStackBottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.HomeGraph,
            modifier = Modifier.padding(innerPadding)
        ) {
            navigation<Destination.HomeGraph>(startDestination = Destination.Home) {
                composable<Destination.Home> { HomePlaceholder() }
                gameDetailDestination()
            }

            navigation<Destination.SearchGraph>(startDestination = Destination.Search) {
                composable<Destination.Search> {
                    SearchScreen(
                        onNavigateToGameDetail = { gameId -> navController.navigate(Destination.Detail(gameId)) },
                        onNavigateToLibrary = {
                            navController.navigateToBottomNavDestination(Destination.LibraryGraph)
                        }
                    )
                }
                gameDetailDestination()
            }

            navigation<Destination.LibraryGraph>(startDestination = Destination.Library) {
                composable<Destination.Library> { LibraryPlaceholder() }
                gameDetailDestination()
            }
        }
    }
}

// Called from every tab graph, never once at top level — see CLAUDE.md,
// Architecture → Navigation structure.
private fun NavGraphBuilder.gameDetailDestination() {
    composable<Destination.Detail> { DestinationPlaceholder(R.string.detail_placeholder_title) }
}

// Shared with GameStackBottomNavBar's item clicks so in-content actions (e.g.
// Search's "Go To Library") switch tabs identically. Takes a *Graph route only —
// see CLAUDE.md, Architecture → Navigation structure.
internal fun NavHostController.navigateToBottomNavDestination(destination: Destination) {
    navigate(destination) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

// Placeholders until the corresponding feature screens are built (see new-feature skill).
@Composable
private fun HomePlaceholder() = DestinationPlaceholder(R.string.nav_home)

@Composable
private fun LibraryPlaceholder() = DestinationPlaceholder(R.string.nav_library)

@Composable
private fun DestinationPlaceholder(labelRes: Int) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = stringResource(labelRes))
    }
}

@Preview
@Composable
private fun DestinationPlaceHolderPreview() = DestinationPlaceholder(R.string.nav_home)
