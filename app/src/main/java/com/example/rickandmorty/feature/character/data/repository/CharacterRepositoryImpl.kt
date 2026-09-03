package com.example.rickandmorty.feature.character.data.repository

import androidx.paging.PagingSource
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.character.data.remote.CharacterApiService
import com.example.rickandmorty.feature.character.data.mapper.toDomain
import com.example.rickandmorty.feature.character.data.paging.CharacterPagingSource
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val characterApi: CharacterApiService
) : CharacterRepository {

    override fun characterPagingSource(): PagingSource<Int, CharacterModel> =
        CharacterPagingSource(characterApi)

    override suspend fun getCharacterById(id: Int): Resource<CharacterModel> =
        safeApiCall { characterApi.getCharacterById(id).toDomain() }

    override suspend fun getSelectedCharacterList(ids: String): Resource<List<CharacterModel>> =
        safeApiCall { characterApi.getSelectedCharacterList(ids).map { it.toDomain() } }

}
