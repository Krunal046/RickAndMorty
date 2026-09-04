package com.example.rickandmorty.feature.location.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class LocationDTO(
    val id: Int,
    val name: String,
    val type: String,
    val dimension: String,
    /** Character URLs; the ids are parsed off the tails at the data boundary. */
    val residents: List<String>,
    val url: String,
    val created: String
)
