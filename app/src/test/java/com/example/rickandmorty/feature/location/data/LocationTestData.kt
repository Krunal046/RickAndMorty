package com.example.rickandmorty.feature.location.data

import com.example.rickandmorty.feature.character.data.remote.dto.Pagination
import com.example.rickandmorty.feature.location.data.remote.dto.LocationDTO
import com.example.rickandmorty.feature.location.data.remote.dto.LocationInfoDTO

fun locationDto(
    id: Int = 1,
    name: String = "Earth (C-137)",
    type: String = "Planet",
    dimension: String = "Dimension C-137",
    residents: List<String> = listOf(
        "https://rickandmortyapi.com/api/character/38",
        "https://rickandmortyapi.com/api/character/45"
    )
) = LocationDTO(
    id = id,
    name = name,
    type = type,
    dimension = dimension,
    residents = residents,
    url = "https://rickandmortyapi.com/api/location/$id",
    created = "2017-11-10T12:42:04.162Z"
)

/** [next] null is how the API says there are no more pages. */
fun locationPage(
    locations: List<LocationDTO>,
    next: String? = null
) = LocationInfoDTO(
    info = Pagination(count = locations.size, pages = 1, next = next, prev = null),
    results = locations
)
