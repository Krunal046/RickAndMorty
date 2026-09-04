package com.example.rickandmorty.feature.character.domain.usecase

import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * What the detail screen renders (spec C3): the cached character, and null until one is
 * cached. Reading and refreshing are separate use cases so that the screen keeps showing
 * the cache while a refresh is failing.
 */
class ObserveCharacterUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    operator fun invoke(id: Int): Flow<CharacterModel?> = characterRepository.observeCharacter(id)
}
