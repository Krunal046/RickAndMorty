package com.example.rickandmorty.feature.location.domain.usecase

import androidx.paging.PagingData
import com.example.rickandmorty.feature.location.domain.model.LocationModel
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import com.example.rickandmorty.feature.location.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Spec L1 and L3: the paged location list, plain or filtered. */
class GetLocationPagingUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {

    operator fun invoke(query: LocationQuery = LocationQuery()): Flow<PagingData<LocationModel>> =
        locationRepository.locationPaging(query)
}
