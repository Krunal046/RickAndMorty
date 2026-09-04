package com.example.rickandmorty.feature.location.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.location.data.local.dao.LocationDao
import com.example.rickandmorty.feature.location.data.mapper.toDomain
import com.example.rickandmorty.feature.location.data.mapper.toEntity
import com.example.rickandmorty.feature.location.data.paging.LocationRemoteMediator
import com.example.rickandmorty.feature.location.data.remote.LocationApiService
import com.example.rickandmorty.feature.location.data.remote.dto.LocationDTO
import com.example.rickandmorty.feature.location.domain.model.LocationModel
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import com.example.rickandmorty.feature.location.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val locationApi: LocationApiService,
    private val locationDao: LocationDao,
    private val database: RickAndMortyDatabase
) : LocationRepository {

    override fun locationPaging(query: LocationQuery): Flow<PagingData<LocationModel>> {
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
            remoteMediator = LocationRemoteMediator(
                pageQuery = pageQuery,
                database = database,
                locationDao = locationDao,
                fetchPage = { page ->
                    locationApi.getLocationList(
                        page = page,
                        name = query.nameOrNull(),
                        type = query.typeOrNull(),
                        dimension = query.dimensionOrNull()
                    )
                }
            ),
            // A fresh source per call: a PagingSource is single-use and Paging invalidates
            // and re-creates it whenever the table changes.
            pagingSourceFactory = { locationDao.pagingSource(pageQuery) }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override fun observeLocation(id: Int): Flow<LocationModel?> =
        locationDao.observeById(id).map { it?.toDomain() }

    override suspend fun refreshLocation(id: Int): Resource<Unit> = safeApiCall {
        val location = locationApi.getLocationById(id)

        database.withTransaction { cacheDetail(location) }
    }

    /**
     * Writes a freshly fetched location over every cached copy of it at once, as the other
     * two features do and for the same reason.
     *
     * A location has one row per list that loaded it and each row carries that list's
     * position. Upserting a single row would either reorder a list - the detail knows no
     * position and would write 0 - or leave the copy `observeById` happens to return stale.
     * So each existing row is rewritten in place, keeping its own `pageQuery` and
     * `orderInQuery`, and the detail row is added on top for the case where no list has
     * cached this location at all - which is the normal case when arriving from a
     * character's origin link.
     */
    private suspend fun cacheDetail(location: LocationDTO) {
        val detail = location.toEntity(pageQuery = LocationQuery.DETAIL, orderInQuery = 0)

        val rows = locationDao.rowsForId(location.id)
            .map { cached -> detail.copy(pageQuery = cached.pageQuery, orderInQuery = cached.orderInQuery) }
            .plus(detail)
            // The detail row is itself one of the cached copies after the first refresh.
            .distinctBy { it.pageQuery }

        locationDao.upsertAll(rows)
    }

    private companion object {
        /** The page size the Rick and Morty API serves; it is not configurable. */
        const val PAGE_SIZE = 20
        const val PREFETCH_DISTANCE = 5
    }
}
