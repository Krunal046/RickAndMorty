package com.example.rickandmorty.feature.episode.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EpisodeDTO(
    val id: Int,
    val name: String,
    @SerialName("air_date") val airDate: String,
    /** The `S01E01` code. Named `episode` by the API; the season is derived from it. */
    val episode: String,
    val characters: List<String>,
    val url: String,
    val created: String
)
