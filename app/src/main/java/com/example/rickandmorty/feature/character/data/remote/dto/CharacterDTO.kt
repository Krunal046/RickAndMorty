package com.example.rickandmorty.feature.character.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CharacterDTO(
    val id: Int,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val origin: CharacterOriginDTO,
    val location: CharacterLocationDTO,
    val image: String,
    val episode: List<String>,
    val url: String,
    val created: String
)

@Serializable
data class CharacterOriginDTO(
    val name: String,
    val url: String
)

@Serializable
data class CharacterLocationDTO(
    val name: String,
    val url: String
)
