package com.example.rickandmorty.feature.episode.data

import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeDTO

fun episodeDto(
    id: Int = 1,
    name: String = "Pilot",
    code: String = "S01E01",
    characters: List<String> = listOf(
        "https://rickandmortyapi.com/api/character/1",
        "https://rickandmortyapi.com/api/character/2"
    )
) = EpisodeDTO(
    id = id,
    name = name,
    airDate = "December 2, 2013",
    episode = code,
    characters = characters,
    url = "https://rickandmortyapi.com/api/episode/$id",
    created = "2017-11-10T12:56:33.798Z"
)

/** The API's own JSON for one episode, as the batch endpoint sends it. */
fun episodeJson(id: Int = 1, name: String = "Pilot", code: String = "S01E01") = """
    {
      "id": $id,
      "name": "$name",
      "air_date": "December 2, 2013",
      "episode": "$code",
      "characters": ["https://rickandmortyapi.com/api/character/1"],
      "url": "https://rickandmortyapi.com/api/episode/$id",
      "created": "2017-11-10T12:56:33.798Z"
    }
""".trimIndent()
