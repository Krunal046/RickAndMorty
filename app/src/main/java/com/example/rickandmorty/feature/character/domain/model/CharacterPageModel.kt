package com.example.rickandmorty.feature.character.domain.model

data class CharacterPageModel(
    val characters: List<CharacterModel>,
    val pagination: PaginationModel
)
