package com.example.rickandmorty.feature.character.domain.model

/** A character's last known location. See [CharacterOriginModel] for the null [id] rule. */
data class CharacterLocationModel(
    val name: String,
    val id: Int?
)
