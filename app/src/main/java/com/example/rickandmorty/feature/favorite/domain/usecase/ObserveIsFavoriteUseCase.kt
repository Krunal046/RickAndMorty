package com.example.rickandmorty.feature.favorite.domain.usecase

import com.example.rickandmorty.feature.favorite.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Whether one character is saved, so the detail screen's heart can fill itself in. */
class ObserveIsFavoriteUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {

    operator fun invoke(characterId: Int): Flow<Boolean> =
        favoriteRepository.observeIsFavorite(characterId)
}
