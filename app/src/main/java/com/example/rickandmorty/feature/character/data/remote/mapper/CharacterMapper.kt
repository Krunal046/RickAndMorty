package com.example.rickandmorty.feature.character.data.remote.mapper

import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterLocationDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterOriginDTO
import com.example.rickandmorty.feature.character.domain.model.CharacterLocationModel
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterOriginModel

fun CharacterDTO.toDomain(): CharacterModel = CharacterModel(
    id = id,
    name = name,
    status = status,
    species = species,
    type = type,
    gender = gender,
    origin = origin.toDomain(),
    location = location.toDomain(),
    image = image,
    episode = episode,
    url = url,
    created = created
)

fun CharacterOriginDTO.toDomain(): CharacterOriginModel = CharacterOriginModel(
    name = name,
    url = url
)

fun CharacterLocationDTO.toDomain(): CharacterLocationModel = CharacterLocationModel(
    name = name,
    url = url
)
