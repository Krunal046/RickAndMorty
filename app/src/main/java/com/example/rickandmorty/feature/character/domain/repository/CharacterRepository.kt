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

    /**
     * The cached character, or null while nothing has been cached for [id] yet.
     *
     * The detail screen renders this and nothing else, so it opens from the cache offline.
     * There is deliberately no call that hands the network's answer straight to the UI:
     * [refreshCharacter] writes to the database and this flow reports the result.
     */
    fun observeCharacter(id: Int): Flow<CharacterModel?>

    /** Fetches `/character/{id}` and writes it to the cache. */
    suspend fun refreshCharacter(id: Int): Resource<Unit>

    /**
     * The cached characters for [ids] - an episode's cast (spec E4), a location's residents
     * (L4). Emits what the database holds, possibly nothing or only some of them, and emits
     * again as [refreshCharacters] fills it in.
     *
     * A grid of a relation has no list identity of its own, but it is still read from the
     * database rather than handed straight from the network: a cast that renders online and
     * blanks offline would be the one screen in the app that does not work from cache.
     */
    fun observeCharactersByIds(ids: List<Int>): Flow<List<CharacterModel>>

    /** Fetches [ids] in one batch call (spec X5) and writes them to the cache. */
    suspend fun refreshCharacters(ids: List<Int>): Resource<Unit>
}
