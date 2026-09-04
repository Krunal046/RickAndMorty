package com.example.rickandmorty.feature.character.domain.model

/** The four values the API documents for gender. See [CharacterStatus] for the parsing rule. */
enum class Gender(val apiValue: String) {
    Female("female"),
    Male("male"),
    Genderless("genderless"),
    Unknown("unknown");

    companion object {
        fun fromApi(value: String): Gender =
            entries.firstOrNull { it.apiValue == value.lowercase() } ?: Unknown
    }
}
