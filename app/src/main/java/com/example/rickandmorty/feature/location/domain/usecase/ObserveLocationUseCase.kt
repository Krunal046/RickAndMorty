package com.example.rickandmorty.feature.location.domain.usecase

import com.example.rickandmorty.feature.location.domain.model.LocationModel
import com.example.rickandmorty.feature.location.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * One location as the cache holds it (spec L2).
 *
 * Reading and refreshing are separate use cases because they are separate concerns: this is
 * what the screen renders, [RefreshLocationUseCase] is what keeps it current.
 */
class ObserveLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {

    operator fun invoke(id: Int): Flow<LocationModel?> = locationRepository.observeLocation(id)
}
