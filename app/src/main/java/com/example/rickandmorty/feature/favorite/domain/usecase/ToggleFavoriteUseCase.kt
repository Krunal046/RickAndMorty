package com.example.rickandmorty.feature.favorite.domain.usecase

import com.example.rickandmorty.feature.favorite.domain.repository.FavoriteRepository
import javax.inject.Inject

/**
 * Saves or removes one character. There is no separate "add" and "remove": both entry
 * points - the detail screen's heart and the tab's remove button - are the same tap on the
 * same fact, and the repository owns which way it goes.
 */
class ToggleFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {

    suspend operator fun invoke(characterId: Int) =
        favoriteRepository.toggleFavorite(characterId)
}
