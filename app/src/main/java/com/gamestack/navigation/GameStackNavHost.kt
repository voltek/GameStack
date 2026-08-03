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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.gamestack.R

@Composable
fun GameStackApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { GameStackBottomNavBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Destination.Home> { HomePlaceholder() }
            composable<Destination.Search> { SearchPlaceholder() }
            composable<Destination.Library> { LibraryPlaceholder() }
        }
    }
}

// Placeholders until the corresponding feature screens are built (see new-feature skill).
@Composable
private fun HomePlaceholder() = DestinationPlaceholder(R.string.nav_home)

@Composable
private fun SearchPlaceholder() = DestinationPlaceholder(R.string.nav_search)

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
