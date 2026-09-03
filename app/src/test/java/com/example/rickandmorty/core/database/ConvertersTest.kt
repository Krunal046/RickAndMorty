package com.example.rickandmorty.core.database

import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `round trips an id list`() {
        val ids = listOf(1, 2, 35, 38)

        assertEquals(ids, converters.toIntList(converters.fromIntList(ids)))
    }

    @Test
    fun `round trips an empty list`() {
        assertEquals(emptyList<Int>(), converters.toIntList(converters.fromIntList(emptyList())))
    }
}
