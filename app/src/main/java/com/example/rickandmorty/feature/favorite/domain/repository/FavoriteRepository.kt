package com.example.rickandmorty.feature.favorite.domain.repository

import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import kotlinx.coroutines.flow.Flow

/**
 * Favorites have no network path at all (spec X3) - there is nothing to refresh and nothing
 * to fail, so none of these return a `Resource`. The tab works offline by construction.
 */
interface FavoriteRepository {

    fun observeFavorites(): Flow<List<CharacterModel>>

    fun observeIsFavorite(characterId: Int): Flow<Boolean>

    /** Saves the character if it is not saved, removes it if it is. */
    suspend fun toggleFavorite(characterId: Int)
}
