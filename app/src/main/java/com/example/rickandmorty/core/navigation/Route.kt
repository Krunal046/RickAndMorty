package com.example.rickandmorty.core.navigation

import kotlinx.serialization.Serializable

/**
 * Every destination in the app, as a type rather than a string.
 *
 * Arguments are constructor parameters, so `Route.CharacterDetail(4)` is checked by the
 * compiler and read back with `toRoute<Route.CharacterDetail>()` - there is no
 * `"character/{characterId}"` template to keep in sync and no manual argument parsing.
 *
 * [CharacterDetail] is declared once and reached from the character list, an episode's
 * cast, a location's residents and favorites.
 */
sealed interface Route {

    @Serializable
    data object Characters : Route

    @Serializable
    data object Episodes : Route

    @Serializable
    data object Locations : Route

    @Serializable
    data object Favorites : Route

    @Serializable
    data class CharacterDetail(val characterId: Int) : Route

    @Serializable
    data class EpisodeDetail(val episodeId: Int) : Route

    @Serializable
    data class LocationDetail(val locationId: Int) : Route
}
