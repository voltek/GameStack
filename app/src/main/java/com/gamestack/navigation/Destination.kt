package com.gamestack.navigation

import kotlinx.serialization.Serializable

// Safe-type routes for Navigation Compose. Detail is intentionally absent here —
// per CLAUDE.md it is reached from a game card in any of the three tabs, not a
// bottom nav destination, and no feature screens exist yet to route into it.
sealed interface Destination {

    @Serializable
    data object Home : Destination

    @Serializable
    data object Search : Destination

    @Serializable
    data object Library : Destination
}
