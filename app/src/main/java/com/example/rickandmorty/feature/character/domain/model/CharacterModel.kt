package com.example.rickandmorty.feature.character.domain.model

/**
 * A character as the app's screens and use cases consume it.
 *
 * `episodeIds` holds ids rather than the API's URLs: the id is what the episode endpoints
 * take, and parsing it once at the data boundary keeps URL handling out of the UI.
 *
 * `status` and `gender` are enums rather than the API's free text, so the UI switches on
 * them exhaustively instead of matching strings.
 */
data class CharacterModel(
    val id: Int,
    val name: String,
    val status: CharacterStatus,
    val species: String,
    val type: String,
    val gender: Gender,
    val origin: CharacterOriginModel,
    val location: CharacterLocationModel,
    val image: String,
    val episodeIds: List<Int>,
    val url: String,
    val created: String
)
