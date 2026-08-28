package com.example.rickandmorty.feature.character.domain.repository

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterPageModel

interface CharacterRepository {

    suspend fun getCharacterList(): Resource<CharacterPageModel>

    suspend fun getCharacterById(id: Int): Resource<CharacterModel>

    suspend fun getSelectedCharacterList(ids: String): Resource<List<CharacterModel>>

}
