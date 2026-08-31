package com.example.rickandmorty.feature.character.domain.repository

import androidx.paging.PagingSource
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.model.CharacterModel

interface CharacterRepository {

    /**
     * A fresh source per call: a [PagingSource] is single-use and Paging invalidates and
     * re-creates it on every refresh.
     */
    fun characterPagingSource(): PagingSource<Int, CharacterModel>

    suspend fun getCharacterById(id: Int): Resource<CharacterModel>

    suspend fun getSelectedCharacterList(ids: String): Resource<List<CharacterModel>>

}
