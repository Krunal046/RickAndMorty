package com.example.rickandmorty.feature.character.domain.usecase

import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import javax.inject.Inject

class GetCharacterUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    suspend fun getCharacterList(): CharacterInfoDTO = characterRepository.getCharacterList()

}



