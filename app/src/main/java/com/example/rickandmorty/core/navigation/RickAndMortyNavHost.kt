package com.example.rickandmorty.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.rickandmorty.core.ui.PlaceholderScreen
import com.example.rickandmorty.feature.character.presentation.CharacterDetailRoute
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

        // Declared once and pushed from every tab; the id travels in the route itself, so
        // the screen's ViewModel reads it rather than taking it as a parameter.
        composable<Route.CharacterDetail> {
            CharacterDetailRoute(
                onBackClick = { navController.navigateUp() },
                onEpisodeClick = { navController.navigate(Route.EpisodeDetail(it)) },
                onLocationClick = { navController.navigate(Route.LocationDetail(it)) }
            )
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
