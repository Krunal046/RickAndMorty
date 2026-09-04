package com.example.rickandmorty.feature.episode.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.common.toIdPath
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.core.network.decodeBatch
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.mapper.toDomain
import com.example.rickandmorty.feature.episode.data.mapper.toEntity
import com.example.rickandmorty.feature.episode.data.paging.EpisodeRemoteMediator
import com.example.rickandmorty.feature.episode.data.remote.EpisodeApiService
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeDTO
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class EpisodeRepositoryImpl @Inject constructor(
    private val episodeApi: EpisodeApiService,
    private val episodeDao: EpisodeDao,
    private val database: RickAndMortyDatabase,
    private val json: Json
) : EpisodeRepository {

    override fun episodePaging(query: EpisodeQuery): Flow<PagingData<EpisodeModel>> {
        val pageQuery = query.cacheKey

        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                // Paging defaults initialLoadSize to 3x pageSize, but the API's page size is
                // fixed and it ignores loadSize - asking for more only triggers extra loads.
                initialLoadSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            remoteMediator = EpisodeRemoteMediator(
                pageQuery = pageQuery,
                database = database,
                episodeDao = episodeDao,
                fetchPage = { page ->
                    episodeApi.getEpisodeList(
                        page = page,
                        name = query.nameOrNull(),
                        episode = query.codeOrNull()
                    )
                }
            ),
            // A fresh source per call: a PagingSource is single-use and Paging invalidates
            // and re-creates it whenever the table changes.
            pagingSourceFactory = { episodeDao.pagingSource(pageQuery) }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

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
                        pageQuery = EpisodeQuery.BY_ID,
                        // No list, so no list position; see EpisodeEntity.orderInQuery.
                        orderInQuery = dto.id
                    )
                }
            )
        }
    }

    override fun observeEpisode(id: Int): Flow<EpisodeModel?> =
        episodeDao.observeById(id).map { it?.toDomain() }

    override suspend fun refreshEpisode(id: Int): Resource<Unit> = safeApiCall {
        val episode = episodeApi.getEpisodeById(id)

        database.withTransaction { cacheDetail(episode) }
    }

    /**
     * Writes a freshly fetched episode over every cached copy of it at once, exactly as
     * `CharacterRepositoryImpl.cacheDetail` does for a character and for the same reason.
     *
     * An episode has one row per list that loaded it and each row carries that list's
     * position. Upserting a single row would either reorder a list - the detail knows no
     * position and would write 0 - or leave the copy `observeById` happens to return stale.
     * So each existing row is rewritten in place, keeping its own `pageQuery` and
     * `orderInQuery`, and the detail row is added on top for the case where no list has
     * cached this episode at all.
     */
    private suspend fun cacheDetail(episode: EpisodeDTO) {
        val detail = episode.toEntity(pageQuery = EpisodeQuery.DETAIL, orderInQuery = 0)

        val rows = episodeDao.rowsForId(episode.id)
            .map { cached -> detail.copy(pageQuery = cached.pageQuery, orderInQuery = cached.orderInQuery) }
            .plus(detail)
            // The detail row is itself one of the cached copies after the first refresh.
            .distinctBy { it.pageQuery }

        episodeDao.upsertAll(rows)
    }

    private companion object {
        /** The page size the Rick and Morty API serves; it is not configurable. */
        const val PAGE_SIZE = 20
        const val PREFETCH_DISTANCE = 5
    }
}
