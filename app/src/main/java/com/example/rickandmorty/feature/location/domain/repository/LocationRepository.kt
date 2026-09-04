package com.example.rickandmorty.feature.location.domain.repository

import androidx.paging.PagingData
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.location.domain.model.LocationModel
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import kotlinx.coroutines.flow.Flow

interface LocationRepository {

    /**
     * The location list for [query], read from the local database and kept current in the
     * background. A filtered query is cached and paged exactly like the unfiltered one, so a
     * recent filter still works offline.
     *
     * The repository owns the Pager because assembling one means wiring a DAO to a
     * RemoteMediator - both data-layer concerns the domain layer must not see.
     */
    fun locationPaging(query: LocationQuery = LocationQuery()): Flow<PagingData<LocationModel>>

    /**
     * The cached location, or null while nothing has been cached for [id] yet.
     *
     * The detail screen renders this and nothing else, so it opens from the cache offline.
     */
    fun observeLocation(id: Int): Flow<LocationModel?>

    /** Fetches `/location/{id}` and writes it to the cache. */
    suspend fun refreshLocation(id: Int): Resource<Unit>
}
