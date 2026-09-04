package com.example.rickandmorty.feature.character.domain.usecase

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import javax.inject.Inject

/** Fills the character cache for [ids] in one batch call. */
class RefreshCharactersUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    suspend operator fun invoke(ids: List<Int>): Resource<Unit> =
        characterRepository.refreshCharacters(ids)
}
