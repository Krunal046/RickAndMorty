package com.example.rickandmorty.feature.location.data.remote.dto

import com.example.rickandmorty.feature.character.data.remote.dto.Pagination
import kotlinx.serialization.Serializable

/**
 * A page of locations. Reuses the character feature's [Pagination] rather than declaring a
 * third identical one: it is the API's envelope, the same on every paged endpoint.
 */
@Serializable
data class LocationInfoDTO(
    val info: Pagination,
    val results: List<LocationDTO>
)
