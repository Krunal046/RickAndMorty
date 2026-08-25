package com.example.rickandmorty.feature.character.domain.model

data class PaginationModel(
    val count: Int,
    val pages: Int,
    val next: String?,
    val prev: String?
)


