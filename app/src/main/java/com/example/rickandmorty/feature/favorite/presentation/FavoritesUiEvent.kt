package com.example.rickandmorty.feature.favorite.presentation

sealed interface FavoritesUiEvent {

    data class CharacterClicked(val characterId: Int) : FavoritesUiEvent

    /**
     * The same toggle as the heart on the detail screen. Removing from this tab is the only
     * thing it can mean here, but it goes through the one toggle so the two entry points
     * cannot drift apart.
     */
    data class FavoriteToggled(val characterId: Int) : FavoritesUiEvent
}
