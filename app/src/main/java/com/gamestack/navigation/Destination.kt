package com.gamestack.navigation

import kotlinx.serialization.Serializable

// Safe-type routes for Navigation Compose. Each tab is a nested graph wrapping
// its screen — still exactly three tabs; the graphs are structure, not extra
// destinations. See CLAUDE.md, Architecture → Navigation structure.
sealed interface Destination {

    @Serializable
    data object HomeGraph : Destination

    @Serializable
    data object Home : Destination

    @Serializable
    data object SearchGraph : Destination

    @Serializable
    data object Search : Destination

    @Serializable
    data object LibraryGraph : Destination

    @Serializable
    data object Library : Destination

    // The Detail screen itself is future work; this route exists now so Search's
    // navigation effect has somewhere real to land (placeholder in GameStackNavHost).
    @Serializable
    data class Detail(val gameId: Int) : Destination
}
