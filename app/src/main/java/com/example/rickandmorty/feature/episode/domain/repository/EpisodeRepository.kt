package com.example.rickandmorty.feature.episode.domain.repository

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import kotlinx.coroutines.flow.Flow

interface EpisodeRepository {

    /**
     * The cached episodes for [ids]. Emits what the database holds - possibly nothing, or
     * only some of them - and emits again as [refreshEpisodes] fills it in.
     */
    fun observeEpisodes(ids: List<Int>): Flow<List<EpisodeModel>>

    /** Fetches [ids] in one batch call and writes them to the cache. */
    suspend fun refreshEpisodes(ids: List<Int>): Resource<Unit>
}
