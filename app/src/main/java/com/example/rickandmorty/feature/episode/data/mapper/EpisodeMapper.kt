package com.example.rickandmorty.feature.episode.data.mapper

import com.example.rickandmorty.core.common.idsFromUrls
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeDTO
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel

/** DTO -> entity -> domain, the same chain the character feature uses. */
fun EpisodeDTO.toEntity(pageQuery: String, orderInQuery: Int): EpisodeEntity = EpisodeEntity(
    id = id,
    pageQuery = pageQuery,
    name = name,
    airDate = airDate,
    // `episode` is the API's name for the S01E01 code; `code` is what it actually is.
    code = episode,
    // Stored as ids, not URLs: the cast grid needs them to call /character/{ids}.
    characterIds = characters.idsFromUrls(),
    url = url,
    created = created,
    orderInQuery = orderInQuery
)

fun EpisodeEntity.toDomain(): EpisodeModel = EpisodeModel(
    id = id,
    name = name,
    airDate = airDate,
    code = code,
    characterIds = characterIds,
    url = url,
    created = created
)
