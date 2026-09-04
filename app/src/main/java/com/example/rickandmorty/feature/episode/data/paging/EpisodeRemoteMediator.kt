package com.example.rickandmorty.feature.episode.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.core.common.DataErrorException
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.database.RemoteKeyEntity
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import com.example.rickandmorty.feature.episode.data.mapper.toEntity
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeDTO
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeInfoDTO
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery

/**
 * Keeps the `episodes` table stocked for one query; it never hands data to the UI.
 *
 * This is `CharacterRemoteMediator` applied to the other resource, deliberately so: spec C1
 * calls the character list "the reference implementation every later list copies", and the
 * rules it encodes - a 404 is an empty result, rows and cursor move in one transaction, old
 * searches are evicted - are as true of episodes as of characters.
 *
 * Everything is scoped by [pageQuery], so the plain list and each search keep their own rows
 * and their own cursor and cannot overwrite one another.
 */
@OptIn(ExperimentalPagingApi::class)
class EpisodeRemoteMediator(
    private val pageQuery: String,
    private val database: RickAndMortyDatabase,
    private val episodeDao: EpisodeDao,
    private val fetchPage: suspend (page: Int) -> EpisodeInfoDTO
) : RemoteMediator<Int, EpisodeEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, EpisodeEntity>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> FIRST_PAGE

            // The API only pages forward and the cache always starts at page 1, so there is
            // never anything above the first row to load.
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)

            // A stored null means the API already reported info.next == null.
            LoadType.APPEND -> database.remoteKeyDao().remoteKey(pageQuery)?.nextPage
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        }

        return when (val result = safeApiCall { fetchPage(page) }) {
            /**
             * Spec §8: a search that matches nothing answers
             * `404 {"error":"There is nothing here"}`. That is an empty result, not a
             * failure - surfacing it as an error would put a retry button in front of a user
             * whose search simply had no hits. Writing an empty page also clears any rows the
             * previous query left behind, so the screen shows its empty state.
             */
            is Resource.Error -> if (result.error == DataError.Http(HTTP_NOT_FOUND)) {
                writePage(loadType, episodes = emptyList(), nextPage = null)
                MediatorResult.Success(endOfPaginationReached = true)
            } else {
                MediatorResult.Error(DataErrorException(result.error))
            }

            is Resource.Success -> {
                // info.next, not a page count, is what the API says ends the list.
                val nextPage = if (result.data.info.next == null) null else page + 1
                writePage(loadType, result.data.results, nextPage)
                MediatorResult.Success(endOfPaginationReached = nextPage == null)
            }
        }
    }

    /**
     * One transaction, so the rows and the cursor can never disagree. If this is interrupted
     * the next load sees the previous consistent state rather than a half-applied page.
     */
    private suspend fun writePage(
        loadType: LoadType,
        episodes: List<EpisodeDTO>,
        nextPage: Int?
    ) {
        database.withTransaction {
            if (loadType == LoadType.REFRESH) {
                episodeDao.clearForQuery(pageQuery)
                database.remoteKeyDao().clear(pageQuery)
            }

            // Continue the existing ordering on append; a refresh has just emptied the table.
            val startOrder = (episodeDao.maxOrder(pageQuery) ?: -1) + 1

            episodeDao.upsertAll(
                episodes.mapIndexed { index, dto ->
                    dto.toEntity(pageQuery = pageQuery, orderInQuery = startOrder + index)
                }
            )

            database.remoteKeyDao().upsert(
                RemoteKeyEntity(
                    queryKey = pageQuery,
                    nextPage = nextPage,
                    lastUpdated = System.currentTimeMillis()
                )
            )

            if (loadType == LoadType.REFRESH) trimStaleQueries()
        }
    }

    /**
     * Every distinct search caches under its own key, so the table would otherwise grow for
     * the lifetime of the install. Keeping the most recently used searches is enough to make
     * going back to a recent one work offline, which is the point of caching them at all.
     * The unfiltered list is never evicted.
     *
     * `EpisodeQuery.DETAIL` and `EpisodeQuery.BY_ID` are safe from this by construction:
     * neither is ever written to `remote_keys`, so neither can be returned here.
     */
    private suspend fun trimStaleQueries() {
        val stale = database.remoteKeyDao()
            .staleFilteredKeys(resource = EpisodeQuery.RESOURCE, keep = MAX_CACHED_SEARCHES)

        if (stale.isNotEmpty()) {
            episodeDao.clearForQueries(stale)
            database.remoteKeyDao().clearAll(stale)
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val HTTP_NOT_FOUND = 404

        /** Searches kept on disk, most recently used first. */
        const val MAX_CACHED_SEARCHES = 10
    }
}
