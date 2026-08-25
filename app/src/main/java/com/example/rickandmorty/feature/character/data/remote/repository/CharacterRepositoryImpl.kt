package com.example.rickandmorty.feature.character.data.remote.repository

import com.example.rickandmorty.feature.character.data.remote.CharacterApiService
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepositoryImpl @Inject constructor(
    val characterApi: CharacterApiService
): CharacterRepository {
    override suspend fun getCharacterList(): CharacterInfoDTO {
        return characterApi.getCharacterList()
    }

    override suspend fun getCharacterById(id: Int): CharacterDTO {
        return characterApi.getCharacterById(id)
    }

    override suspend fun getSelectedCharacterList(ids: String): List<CharacterDTO> {
        return characterApi.getSelectedCharacterList(ids)
    }

}







