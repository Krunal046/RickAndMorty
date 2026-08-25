package com.example.rickandmorty.feature.character.domain.repository

import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO

interface CharacterRepository {

    suspend fun getCharacterList(): CharacterInfoDTO

    suspend fun getCharacterById(id: Int): CharacterDTO

    suspend fun getSelectedCharacterList(ids: String): List<CharacterDTO>

}




