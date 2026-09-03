package com.example.rickandmorty.feature.character.domain.repository

import androidx.paging.PagingData
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {

    /**
     * The character list for [query], read from the local database and kept current in the
     * background. A filtered query is cached and paged exactly like the unfiltered one, so
     * a recent search still works offline.
     *
     * The repository owns the Pager because assembling one means wiring a DAO to a
     * RemoteMediator - both data-layer concerns that the domain layer must not see.
     */
    fun characterPaging(query: CharacterQuery = CharacterQuery()): Flow<PagingData<CharacterModel>>

    suspend fun getCharacterById(id: Int): Resource<CharacterModel>

    suspend fun getSelectedCharacterList(ids: String): Resource<List<CharacterModel>>
}
