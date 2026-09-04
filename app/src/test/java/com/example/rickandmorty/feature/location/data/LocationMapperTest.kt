package com.example.rickandmorty.feature.location.data

import com.example.rickandmorty.feature.location.data.mapper.toDomain
import com.example.rickandmorty.feature.location.data.mapper.toEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationMapperTest {

    /** Spec L4 calls `/character/{ids}`, so the URLs have to become ids at the boundary. */
    @Test
    fun `resident urls become ids`() {
        val entity = locationDto().toEntity(pageQuery = "location", orderInQuery = 0)

        assertEquals(listOf(38, 45), entity.residentIds)
    }

    /** A malformed URL drops the one relation rather than failing the whole parse. */
    @Test
    fun `a resident url without an id is skipped`() {
        val dto = locationDto(
            residents = listOf(
                "https://rickandmortyapi.com/api/character/1",
                "https://rickandmortyapi.com/api/character/oops"
            )
        )

        assertEquals(listOf(1), dto.toEntity("location", 0).residentIds)
    }

    @Test
    fun `a location with nobody living in it maps to an empty list`() {
        assertEquals(
            emptyList<Int>(),
            locationDto(residents = emptyList()).toEntity("location", 0).residentIds
        )
    }

    @Test
    fun `the round trip to the domain model keeps every field`() {
        val model = locationDto().toEntity(pageQuery = "location", orderInQuery = 3).toDomain()

        assertEquals(1, model.id)
        assertEquals("Earth (C-137)", model.name)
        assertEquals("Planet", model.type)
        assertEquals("Dimension C-137", model.dimension)
        assertEquals(listOf(38, 45), model.residentIds)
    }
}
