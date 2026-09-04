package com.example.rickandmorty.feature.episode.data.repository

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.common.toIdPath
import com.example.rickandmorty.core.network.decodeBatch
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import com.example.rickandmorty.feature.episode.data.mapper.toDomain
import com.example.rickandmorty.feature.episode.data.mapper.toEntity
import com.example.rickandmorty.feature.episode.data.remote.EpisodeApiService
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeDTO
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val episodeApi: EpisodeApiService,
    private val episodeDao: EpisodeDao,
    private val json: Json
) : EpisodeRepository {

    override fun observeEpisodes(ids: List<Int>): Flow<List<EpisodeModel>> =
        if (ids.isEmpty()) {
            flowOf(emptyList())
        } else {
            episodeDao.observeByIds(ids).map { rows -> rows.map { it.toDomain() } }
        }

    /**
     * The empty case has to short-circuit rather than build a path: `episode/` is the
     * *paged list* endpoint, so asking for no episodes would quietly download page one.
     */
    override suspend fun refreshEpisodes(ids: List<Int>): Resource<Unit> {
        if (ids.isEmpty()) return Resource.Success(Unit)

        return safeApiCall {
            val episodes = json.decodeBatch<EpisodeDTO>(episodeApi.getEpisodesByIds(ids.toIdPath()))

            episodeDao.upsertAll(
                episodes.map { dto ->
                    dto.toEntity(
                        pageQuery = EpisodeEntity.BY_ID_QUERY,
                        // No list, so no list position; see EpisodeEntity.orderInQuery.
                        orderInQuery = dto.id
                    )
                }
            )
        }
    }
}
