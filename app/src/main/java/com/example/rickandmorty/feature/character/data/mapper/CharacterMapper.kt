package com.example.rickandmorty.feature.character.data.mapper

import com.example.rickandmorty.core.common.idsFromUrls
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.domain.model.CharacterLocationModel
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterOriginModel

/**
 * DTO -> entity -> domain. Each layer owns its own model, and these are the only places
 * that know about more than one of them, so a change to the JSON stops here.
 *
 * The DTO is never mapped straight to the domain for a list: everything the UI reads comes
 * out of the database, so the network's output is written as an entity and read back.
 */
fun CharacterDTO.toEntity(pageQuery: String, orderInQuery: Int): CharacterEntity = CharacterEntity(
    id = id,
    pageQuery = pageQuery,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    originName = origin.name,
    originUrl = origin.url,
    lastLocationName = location.name,
    lastLocationUrl = location.url,
    imageUrl = image,
    // Stored as ids, not URLs: the detail screen needs them to call /episode/{ids}.
    episodeIds = episode.idsFromUrls(),
    url = url,
    created = created,
    orderInQuery = orderInQuery
)

fun CharacterEntity.toDomain(): CharacterModel = CharacterModel(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    origin = CharacterOriginModel(name = originName, url = originUrl),
    location = CharacterLocationModel(name = lastLocationName, url = lastLocationUrl),
    image = imageUrl,
    episodeIds = episodeIds,
    url = url,
    created = created
)

/**
 * For the single and multi-id endpoints, whose results are not part of a paged list and so
 * have no query to belong to.
 */
fun CharacterDTO.toDomain(): CharacterModel =
    toEntity(pageQuery = "", orderInQuery = 0).toDomain()
