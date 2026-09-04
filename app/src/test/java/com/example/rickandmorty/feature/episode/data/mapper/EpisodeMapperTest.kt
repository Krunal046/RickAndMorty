package com.example.rickandmorty.feature.episode.data.mapper

import com.example.rickandmorty.feature.episode.data.episodeDto
import org.junit.Assert.assertEquals
import org.junit.Test

class EpisodeMapperTest {

    /** The API calls the `S01E01` string `episode`; the entity calls it what it is. */
    @Test
    fun `keeps the season and episode code`() {
        val entity = episodeDto(code = "S02E04").toEntity(pageQuery = "episode", orderInQuery = 0)

        assertEquals("S02E04", entity.code)
        assertEquals("S02E04", entity.toDomain().code)
    }

    @Test
    fun `parses cast ids off the character urls`() {
        val dto = episodeDto(
            characters = listOf(
                "https://rickandmortyapi.com/api/character/1",
                "https://rickandmortyapi.com/api/character/35"
            )
        )

        assertEquals(
            listOf(1, 35),
            dto.toEntity(pageQuery = "episode", orderInQuery = 0).characterIds
        )
    }

    @Test
    fun `entity to domain preserves every field the ui reads`() {
        val domain = episodeDto(id = 3, name = "Anatomy Park")
            .toEntity(pageQuery = "episode", orderInQuery = 2)
            .toDomain()

        assertEquals(3, domain.id)
        assertEquals("Anatomy Park", domain.name)
        assertEquals("December 2, 2013", domain.airDate)
        assertEquals("S01E01", domain.code)
    }
}
