package com.example.rickandmorty.feature.episode.domain.model

/**
 * An episode as the app's screens consume it.
 *
 * [code] is the API's `S01E01` string; Phase 6 groups the episode list by the season it
 * carries, and the detail screen shows it as-is.
 */
data class EpisodeModel(
    val id: Int,
    val name: String,
    val airDate: String,
    val code: String,
    val characterIds: List<Int>,
    val url: String,
    val created: String
)
