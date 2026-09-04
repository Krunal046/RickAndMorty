package com.example.rickandmorty.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.rickandmorty.R

/** The four bottom-bar tabs, in the order the spec lists them. */
enum class TopLevelDestination(
    val route: Route,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector
) {
    CHARACTERS(Route.Characters, R.string.tab_characters, Icons.Filled.Person),
    EPISODES(Route.Episodes, R.string.tab_episodes, Icons.Filled.PlayArrow),
    LOCATIONS(Route.Locations, R.string.tab_locations, Icons.Filled.Place),
    FAVORITES(Route.Favorites, R.string.tab_favorites, Icons.Filled.Favorite)
}
