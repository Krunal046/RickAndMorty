package com.example.rickandmorty.feature.favorite.domain.usecase

import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.favorite.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** What the Favorites tab renders (spec S8), newest first. */
class ObserveFavoriteCharactersUseCase @Inject constructor(
    private val favoriteRepository: FavoriteRepository
) {

    operator fun invoke(): Flow<List<CharacterModel>> = favoriteRepository.observeFavorites()
}
