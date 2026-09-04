package com.example.rickandmorty.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.rickandmorty.feature.character.presentation.CharacterDetailRoute
import com.example.rickandmorty.feature.character.presentation.CharacterRoute
import com.example.rickandmorty.feature.episode.presentation.EpisodeDetailRoute
import com.example.rickandmorty.feature.episode.presentation.EpisodeRoute
import com.example.rickandmorty.feature.favorite.presentation.FavoritesRoute
import com.example.rickandmorty.feature.location.presentation.LocationDetailRoute
import com.example.rickandmorty.feature.location.presentation.LocationRoute

/**
 * The whole graph in one place. Screens receive plain navigation lambdas rather than the
 * [NavHostController] itself, so no feature depends on navigation and each screen stays
 * previewable and testable on its own.
 *
 * Every destination in [Route] is implemented as of Phase 7; see `doc/FEATURES.md`.
 */
@Composable
fun RickAndMortyNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val toCharacterDetail: (Int) -> Unit = { navController.navigate(Route.CharacterDetail(it)) }
    val toEpisodeDetail: (Int) -> Unit = { navController.navigate(Route.EpisodeDetail(it)) }
    val toLocationDetail: (Int) -> Unit = { navController.navigate(Route.LocationDetail(it)) }

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
                onEpisodeClick = toEpisodeDetail,
                onLocationClick = toLocationDetail
            )
        }

        // ---- Episodes tab ---------------------------------------------------------
        composable<Route.Episodes> {
            EpisodeRoute(onEpisodeClick = toEpisodeDetail)
        }

        // Reached from the tab and from a character's episode chips, so the cast it shows
        // pushes character details onto whichever tab the user arrived on.
        composable<Route.EpisodeDetail> {
            EpisodeDetailRoute(
                onBackClick = { navController.navigateUp() },
                onCharacterClick = toCharacterDetail
            )
        }

        // ---- Locations tab --------------------------------------------------------
        composable<Route.Locations> {
            LocationRoute(onLocationClick = toLocationDetail)
        }

        // Also where a character's origin and last-location links land (spec C3), which is
        // why they stop being dead rows in this phase.
        composable<Route.LocationDetail> {
            LocationDetailRoute(
                onBackClick = { navController.navigateUp() },
                onCharacterClick = toCharacterDetail
            )
        }

        // ---- Favorites tab --------------------------------------------------------
        composable<Route.Favorites> {
            FavoritesRoute(onCharacterClick = toCharacterDetail)
        }
    }
}
