package com.example.rickandmorty.feature.location.data.paging

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
import com.example.rickandmorty.feature.location.data.local.dao.LocationDao
import com.example.rickandmorty.feature.location.data.local.entity.LocationEntity
import com.example.rickandmorty.feature.location.data.mapper.toEntity
import com.example.rickandmorty.feature.location.data.remote.dto.LocationDTO
import com.example.rickandmorty.feature.location.data.remote.dto.LocationInfoDTO
import com.example.rickandmorty.feature.location.domain.model.LocationQuery

/**
 * Keeps the `locations` table stocked for one query; it never hands data to the UI.
 *
 * The third application of the rules spec C1 calls "the reference implementation every later
 * list copies": a 404 is an empty result, rows and cursor move in one transaction, and old
 * filtered searches are evicted so the table cannot grow without bound.
 *
 * Everything is scoped by [pageQuery], so the plain list and each filtered search keep their
 * own rows and their own cursor and cannot overwrite one another.
 */
@OptIn(ExperimentalPagingApi::class)
class LocationRemoteMediator(
    private val pageQuery: String,
    private val database: RickAndMortyDatabase,
    private val locationDao: LocationDao,
    private val fetchPage: suspend (page: Int) -> LocationInfoDTO
) : RemoteMediator<Int, LocationEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, LocationEntity>
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
             * Spec §8: a filter that matches nothing answers
             * `404 {"error":"There is nothing here"}`. That is an empty result, not a
             * failure - surfacing it as an error would put a retry button in front of a user
             * whose search simply had no hits. Writing an empty page also clears any rows the
             * previous query left behind, so the screen shows its empty state.
             */
            is Resource.Error -> if (result.error == DataError.Http(HTTP_NOT_FOUND)) {
                writePage(loadType, locations = emptyList(), nextPage = null)
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
        locations: List<LocationDTO>,
        nextPage: Int?
    ) {
        database.withTransaction {
            if (loadType == LoadType.REFRESH) {
                locationDao.clearForQuery(pageQuery)
                database.remoteKeyDao().clear(pageQuery)
            }

            // Continue the existing ordering on append; a refresh has just emptied the table.
            val startOrder = (locationDao.maxOrder(pageQuery) ?: -1) + 1

            locationDao.upsertAll(
                locations.mapIndexed { index, dto ->
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
     * Every distinct filter combination caches under its own key, so the table would
     * otherwise grow for the lifetime of the install. The unfiltered list is never evicted,
     * and `LocationQuery.DETAIL` is out of reach by construction - it is never written to
     * `remote_keys`, so it can never be returned here. That matters more for locations than
     * for the other two: a character's origin link (spec C3) opens a detail that no list may
     * ever have loaded, and evicting it would break the link.
     */
    private suspend fun trimStaleQueries() {
        val stale = database.remoteKeyDao()
            .staleFilteredKeys(resource = LocationQuery.RESOURCE, keep = MAX_CACHED_FILTERS)

        if (stale.isNotEmpty()) {
            locationDao.clearForQueries(stale)
            database.remoteKeyDao().clearAll(stale)
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
        const val HTTP_NOT_FOUND = 404

        /** Filtered searches kept on disk, most recently used first. */
        const val MAX_CACHED_FILTERS = 10
    }
}
