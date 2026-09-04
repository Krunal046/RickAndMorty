package com.example.rickandmorty.feature.location.data.mapper

import com.example.rickandmorty.core.common.idsFromUrls
import com.example.rickandmorty.feature.location.data.local.entity.LocationEntity
import com.example.rickandmorty.feature.location.data.remote.dto.LocationDTO
import com.example.rickandmorty.feature.location.domain.model.LocationModel

/** DTO -> entity -> domain, the same chain the other two features use. */
fun LocationDTO.toEntity(pageQuery: String, orderInQuery: Int): LocationEntity = LocationEntity(
    id = id,
    pageQuery = pageQuery,
    name = name,
    type = type,
    dimension = dimension,
    // Stored as ids, not URLs: the residents grid needs them to call /character/{ids}.
    residentIds = residents.idsFromUrls(),
    url = url,
    created = created,
    orderInQuery = orderInQuery
)

fun LocationEntity.toDomain(): LocationModel = LocationModel(
    id = id,
    name = name,
    type = type,
    dimension = dimension,
    residentIds = residentIds,
    url = url,
    created = created
)
