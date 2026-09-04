package com.example.rickandmorty.feature.character.domain

import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CharacterQueryTest {

    @Test
    fun `an empty query is the plain list`() {
        assertEquals("character", CharacterQuery().cacheKey)
        assertTrue(CharacterQuery().isEmpty)
    }

    @Test
    fun `filters combine into one key, in a fixed order`() {
        val query = CharacterQuery(
            name = "beth",
            status = CharacterStatus.Alive,
            gender = Gender.Female
        )

        assertEquals("character:name=beth&status=alive&gender=female", query.cacheKey)
    }

    /**
     * The API matches names case-insensitively, so "Beth" and "beth" are the same list and
     * must not be cached under two keys.
     */
    @Test
    fun `the key is stable across casing and surrounding whitespace`() {
        val typed = CharacterQuery(name = "  Beth  ")
        val canonical = CharacterQuery(name = "beth")

        assertEquals(canonical.cacheKey, typed.cacheKey)
    }

    @Test
    fun `the same filters always produce the same key regardless of how they were set`() {
        val a = CharacterQuery(status = CharacterStatus.Dead, gender = Gender.Male)
        val b = CharacterQuery(gender = Gender.Male, status = CharacterStatus.Dead)

        assertEquals(a.cacheKey, b.cacheKey)
    }

    @Test
    fun `different filters produce different keys`() {
        val alive = CharacterQuery(status = CharacterStatus.Alive).cacheKey
        val dead = CharacterQuery(status = CharacterStatus.Dead).cacheKey

        assertFalse(alive == dead)
    }

    @Test
    fun `blank text is dropped rather than sent as an empty parameter`() {
        val query = CharacterQuery(name = "   ", species = "")

        assertTrue(query.isEmpty)
        assertEquals(null, query.nameOrNull())
        assertEquals(null, query.speciesOrNull())
    }

    /** The badge counts filters, not the search text. */
    @Test
    fun `search text does not count as an active filter`() {
        assertEquals(0, CharacterQuery(name = "rick").activeFilterCount)
        assertEquals(
            2,
            CharacterQuery(name = "rick", status = CharacterStatus.Alive, species = "Human")
                .activeFilterCount
        )
    }
}
