package com.example.rickandmorty.feature.location.domain.model

/**
 * A location as the app's screens consume it.
 *
 * [residentIds] are parsed off the API's resident URLs at the data boundary, so the grid on
 * S7 can call the multi-id character endpoint without handling a URL (spec L4).
 */
data class LocationModel(
    val id: Int,
    val name: String,
    val type: String,
    val dimension: String,
    val residentIds: List<Int>,
    val url: String,
    val created: String
)
