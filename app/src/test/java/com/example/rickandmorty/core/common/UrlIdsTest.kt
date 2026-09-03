package com.example.rickandmorty.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UrlIdsTest {

    @Test
    fun `reads the id off a resource url`() {
        assertEquals(28, "https://rickandmortyapi.com/api/episode/28".idFromUrlOrNull())
        assertEquals(1, "https://rickandmortyapi.com/api/location/1".idFromUrlOrNull())
    }

    @Test
    fun `returns null when the tail is not a number`() {
        assertNull("https://rickandmortyapi.com/api/episode/".idFromUrlOrNull())
        assertNull("".idFromUrlOrNull())
        assertNull("not a url".idFromUrlOrNull())
    }

    @Test
    fun `drops malformed urls instead of failing the whole list`() {
        val urls = listOf(
            "https://rickandmortyapi.com/api/episode/1",
            "https://rickandmortyapi.com/api/episode/",
            "https://rickandmortyapi.com/api/episode/3"
        )

        assertEquals(listOf(1, 3), urls.idsFromUrls())
    }
}
