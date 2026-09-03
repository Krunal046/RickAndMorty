package com.example.rickandmorty.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.rickandmorty.core.ui.PlaceholderScreen
import com.example.rickandmorty.feature.character.presentation.CharacterRoute

/**
 * The whole graph in one place. Screens receive plain navigation lambdas rather than the
 * [NavHostController] itself, so no feature depends on navigation and each screen stays
 * previewable and testable on its own.
 *
 * Destinations still rendering a [PlaceholderScreen] are implemented by a later phase; see
 * `doc/FEATURES.md`.
 */
@Composable
fun RickAndMortyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val toCharacterDetail: (Int) -> Unit = { navController.navigate(Route.CharacterDetail(it)) }

    NavHost(
        navController = navController,
        startDestination = Route.Characters,
        modifier = modifier
    ) {
        // ---- Characters tab -------------------------------------------------------
        composable<Route.Characters> {
            CharacterRoute(onCharacterClick = toCharacterDetail)
        }

        composable<Route.CharacterDetail> { entry ->
            val characterId = entry.toRoute<Route.CharacterDetail>().characterId
            PlaceholderScreen(title = "Character #$characterId")
        }

        // ---- Episodes tab ---------------------------------------------------------
        composable<Route.Episodes> {
            PlaceholderScreen(title = "Episodes")
        }

        composable<Route.EpisodeDetail> { entry ->
            val episodeId = entry.toRoute<Route.EpisodeDetail>().episodeId
            PlaceholderScreen(title = "Episode #$episodeId")
        }

        // ---- Locations tab --------------------------------------------------------
        composable<Route.Locations> {
            PlaceholderScreen(title = "Locations")
        }

        composable<Route.LocationDetail> { entry ->
            val locationId = entry.toRoute<Route.LocationDetail>().locationId
            PlaceholderScreen(title = "Location #$locationId")
        }

        // ---- Favorites tab --------------------------------------------------------
        composable<Route.Favorites> {
            PlaceholderScreen(title = "Favorites")
        }
    }
}
