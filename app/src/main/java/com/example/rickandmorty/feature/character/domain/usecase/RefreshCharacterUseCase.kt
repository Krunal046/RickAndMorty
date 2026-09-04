package com.example.rickandmorty.feature.character.domain.usecase

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import javax.inject.Inject

/** Brings the cached copy of one character up to date. */
class RefreshCharacterUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    suspend operator fun invoke(id: Int): Resource<Unit> =
        characterRepository.refreshCharacter(id)
}
