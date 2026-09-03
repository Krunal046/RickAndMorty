package com.example.rickandmorty.feature.character.domain

import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterStatusTest {

    /** The API is inconsistent: "Alive" and "Dead" are capitalised, "unknown" is not. */
    @Test
    fun `parses the api's inconsistent casing`() {
        assertEquals(CharacterStatus.Alive, CharacterStatus.fromApi("Alive"))
        assertEquals(CharacterStatus.Dead, CharacterStatus.fromApi("dead"))
        assertEquals(CharacterStatus.Unknown, CharacterStatus.fromApi("unknown"))
        assertEquals(Gender.Genderless, Gender.fromApi("Genderless"))
    }

    /** One unrecognised value must not fail the page it arrived in. */
    @Test
    fun `an unrecognised value falls back to unknown`() {
        assertEquals(CharacterStatus.Unknown, CharacterStatus.fromApi("undead"))
        assertEquals(CharacterStatus.Unknown, CharacterStatus.fromApi(""))
        assertEquals(Gender.Unknown, Gender.fromApi("other"))
    }
}
