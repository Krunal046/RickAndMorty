package com.example.rickandmorty.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * The single Activity's content: a [Scaffold] with the four-tab bottom bar, and the
 * navigation graph inside it. Detail screens are pushed onto the active tab's back stack,
 * so the bar stays put - and stays correctly highlighted - while they are open.
 */
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStack.collectAsStateWithLifecycle()
    val activeTab = backStack.activeTab()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = destination == activeTab,
                        onClick = { navController.switchTab(destination) },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = stringResource(destination.labelRes)
                            )
                        },
                        label = { Text(text = stringResource(destination.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        RickAndMortyNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

/**
 * The tab a detail screen belongs to is the one below it on the back stack, not anything
 * about the detail destination itself: `CharacterDetail` is declared once and pushed from
 * all four tabs, so it sits under whichever tab the user opened it from.
 *
 * Reading the innermost top-level entry is what keeps Characters highlighted while a
 * character detail is open, and Episodes highlighted when the same screen was opened from
 * an episode's cast.
 */
private fun List<NavBackStackEntry>.activeTab(): TopLevelDestination? =
    asReversed().firstNotNullOfOrNull { entry ->
        TopLevelDestination.entries.firstOrNull { entry.destination.hasRoute(it.route::class) }
    }

/**
 * Switching tabs saves the outgoing tab's back stack and restores the incoming one, so each
 * tab keeps its own history and a detail screen is still there when the user comes back.
 * `launchSingleTop` keeps a re-tap on the current tab from stacking a second copy.
 */
private fun NavHostController.switchTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
