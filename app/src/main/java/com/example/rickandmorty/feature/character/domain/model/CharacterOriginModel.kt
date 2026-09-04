package com.example.rickandmorty.feature.character.domain.model

/**
 * Where a character is from. [id] is parsed off the API's URL at the data boundary, the
 * same way `CharacterModel.episodeIds` is, so the UI never handles a URL.
 *
 * It is null for the many characters whose origin is `unknown`: the API sends those with an
 * empty url, and there is no location to open. The detail screen renders the name without a
 * link in that case.
 */
data class CharacterOriginModel(
    val name: String,
    val id: Int?
)
