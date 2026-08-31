package com.example.rickandmorty.feature.character.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CharacterInfoDTO(
    val info: Pagination,
    val results: List<CharacterDTO>
)

@Serializable
data class Pagination(
    val count: Int,
    val pages: Int,
    val next: String?,
    val prev: String?
)
