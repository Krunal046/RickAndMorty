package com.example.rickandmorty.feature.character.data

import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterLocationDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterOriginDTO
import com.example.rickandmorty.feature.character.data.remote.dto.Pagination

fun characterDto(
    id: Int = 1,
    name: String = "Rick Sanchez",
    status: String = "Alive",
    episode: List<String> = listOf(
        "https://rickandmortyapi.com/api/episode/1",
        "https://rickandmortyapi.com/api/episode/2"
    )
) = CharacterDTO(
    id = id,
    name = name,
    status = status,
    species = "Human",
    type = "",
    gender = "Male",
    origin = CharacterOriginDTO(
        name = "Earth (C-137)",
        url = "https://rickandmortyapi.com/api/location/1"
    ),
    location = CharacterLocationDTO(
        name = "Citadel of Ricks",
        url = "https://rickandmortyapi.com/api/location/3"
    ),
    image = "https://rickandmortyapi.com/api/character/avatar/$id.jpeg",
    episode = episode,
    url = "https://rickandmortyapi.com/api/character/$id",
    created = "2017-11-04T18:48:46.250Z"
)

/** [next] null is how the API says there are no more pages. */
fun characterPage(
    characters: List<CharacterDTO>,
    next: String? = null
) = CharacterInfoDTO(
    info = Pagination(count = characters.size, pages = 1, next = next, prev = null),
    results = characters
)
