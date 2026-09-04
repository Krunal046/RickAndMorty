package com.example.rickandmorty.feature.favorite.presentation

/** See `CharacterUiEffect` for why navigation is an effect rather than state. */
sealed interface FavoritesUiEffect {

    data class NavigateToCharacterDetail(val characterId: Int) : FavoritesUiEffect
}
