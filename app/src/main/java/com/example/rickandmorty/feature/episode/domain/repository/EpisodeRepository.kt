package com.example.rickandmorty.feature.episode.domain.repository

import androidx.paging.PagingData
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {

    /**
     * The episode list for [query], read from the local database and kept current in the
     * background. A search is cached and paged exactly like the unfiltered list, so a recent
     * one still works offline.
     *
     * The repository owns the Pager because assembling one means wiring a DAO to a
     * RemoteMediator - both data-layer concerns the domain layer must not see.
     */
    fun episodePaging(query: EpisodeQuery = EpisodeQuery()): Flow<PagingData<EpisodeModel>>

    /**
     * The cached episodes for [ids]. Emits what the database holds - possibly nothing, or
     * only some of them - and emits again as [refreshEpisodes] fills it in.
     */
    fun observeEpisodes(ids: List<Int>): Flow<List<EpisodeModel>>

    /** Fetches [ids] in one batch call and writes them to the cache. */
    suspend fun refreshEpisodes(ids: List<Int>): Resource<Unit>

    /**
     * The cached episode, or null while nothing has been cached for [id] yet.
     *
     * The detail screen renders this and nothing else, so it opens from the cache offline.
     */
    fun observeEpisode(id: Int): Flow<EpisodeModel?>

    /** Fetches `/episode/{id}` and writes it to the cache. */
    suspend fun refreshEpisode(id: Int): Resource<Unit>
}
