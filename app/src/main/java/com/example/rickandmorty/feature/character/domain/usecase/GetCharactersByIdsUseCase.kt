package com.example.rickandmorty.feature.character.domain.usecase

import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The characters of an episode's cast (spec E4) or a location's residents (L4), read from
 * the cache.
 *
 * The mirror of `GetEpisodesByIdsUseCase`: reading and refreshing are separate because they
 * are separate concerns.
 */
class GetCharactersByIdsUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    operator fun invoke(ids: List<Int>): Flow<List<CharacterModel>> =
        characterRepository.observeCharactersByIds(ids)
}
