package com.example.rickandmorty.feature.episode.data.remote.dto

import com.example.rickandmorty.feature.character.data.remote.dto.Pagination
import kotlinx.serialization.Serializable

/**
 * A page of episodes. Reuses the character feature's [Pagination] rather than declaring a
 * second identical one: it is the API's envelope, the same on every paged endpoint, and two
 * copies would be two things to keep in step.
 */
@Serializable
data class EpisodeInfoDTO(
    val info: Pagination,
    val results: List<EpisodeDTO>
)
