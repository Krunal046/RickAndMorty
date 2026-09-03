package com.example.rickandmorty.feature.character.domain.repository

import androidx.paging.PagingData
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {

    /**
     * The character list, read from the local database and kept current in the background.
     *
     * The repository owns the Pager because assembling one means wiring a DAO to a
     * RemoteMediator - both data-layer concerns that the domain layer must not see.
     */
    fun characterPaging(): Flow<PagingData<CharacterModel>>

    suspend fun getCharacterById(id: Int): Resource<CharacterModel>

    suspend fun getSelectedCharacterList(ids: String): Resource<List<CharacterModel>>
}
