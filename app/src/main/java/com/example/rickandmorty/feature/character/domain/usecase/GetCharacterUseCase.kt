package com.example.rickandmorty.feature.character.domain.usecase

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.model.CharacterPageModel
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import javax.inject.Inject

class GetCharacterUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    suspend operator fun invoke(): Resource<CharacterPageModel> =
        characterRepository.getCharacterList()

}
