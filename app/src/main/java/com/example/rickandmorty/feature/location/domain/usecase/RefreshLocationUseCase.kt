package com.example.rickandmorty.feature.location.domain.usecase

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.location.domain.repository.LocationRepository
import javax.inject.Inject

/** Fetches one location and writes it to the cache. */
class RefreshLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {

    suspend operator fun invoke(id: Int): Resource<Unit> = locationRepository.refreshLocation(id)
}
