package com.example.rickandmorty.feature.character.domain.model

/**
 * The API sends status as free text and is inconsistent about casing (`Alive`, `unknown`).
 * The domain closes it into the three values the API actually documents, so the UI can
 * switch on it exhaustively instead of comparing strings.
 *
 * [apiValue] is what the filter endpoint expects; anything unrecognised maps to [Unknown]
 * rather than failing the parse, because one odd value should not lose a whole page.
 */
enum class CharacterStatus(val apiValue: String) {
    Alive("alive"),
    Dead("dead"),
    Unknown("unknown");

    companion object {
        fun fromApi(value: String): CharacterStatus =
            entries.firstOrNull { it.apiValue == value.lowercase() } ?: Unknown
    }
}
