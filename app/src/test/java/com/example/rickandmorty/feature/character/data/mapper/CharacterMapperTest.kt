package com.example.rickandmorty.feature.character.data.mapper

import com.example.rickandmorty.feature.character.data.characterDto
import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterMapperTest {

    @Test
    fun `flattens the nested origin and location objects into columns`() {
        val entity = characterDto().toEntity(pageQuery = "character", orderInQuery = 0)

        assertEquals("Earth (C-137)", entity.originName)
        assertEquals("https://rickandmortyapi.com/api/location/1", entity.originUrl)
        assertEquals("Citadel of Ricks", entity.lastLocationName)
        assertEquals("https://rickandmortyapi.com/api/location/3", entity.lastLocationUrl)
    }

    /** Spec §9: related items arrive as URLs and the id is the tail. */
    @Test
    fun `parses episode ids off the urls`() {
        val dto = characterDto(
            episode = listOf(
                "https://rickandmortyapi.com/api/episode/1",
                "https://rickandmortyapi.com/api/episode/28",
                "https://rickandmortyapi.com/api/episode/51"
            )
        )

        val entity = dto.toEntity(pageQuery = "character", orderInQuery = 0)

        assertEquals(listOf(1, 28, 51), entity.episodeIds)
    }

    @Test
    fun `carries the query and ordering the mediator assigned`() {
        val entity = characterDto().toEntity(pageQuery = "character:status=alive", orderInQuery = 7)

        assertEquals("character:status=alive", entity.pageQuery)
        assertEquals(7, entity.orderInQuery)
    }

    /**
     * The entity keeps the API's raw text so the cache mirrors the response; the enum is
     * resolved on the way to the domain (spec §9).
     */
    @Test
    fun `entity stores raw api text and the domain gets the enum`() {
        val entity = characterDto(status = "unknown").toEntity(pageQuery = "character", orderInQuery = 0)

        assertEquals("unknown", entity.status)
        assertEquals(CharacterStatus.Unknown, entity.toDomain().status)
    }

    @Test
    fun `entity to domain preserves every field the ui reads`() {
        val dto = characterDto(id = 4, name = "Beth Smith", status = "Alive")

        val domain = dto.toEntity(pageQuery = "character", orderInQuery = 0).toDomain()

        assertEquals(4, domain.id)
        assertEquals("Beth Smith", domain.name)
        assertEquals(CharacterStatus.Alive, domain.status)
        assertEquals("Human", domain.species)
        assertEquals(Gender.Male, domain.gender)
        assertEquals("Earth (C-137)", domain.origin.name)
        assertEquals("Citadel of Ricks", domain.location.name)
        assertEquals(listOf(1, 2), domain.episodeIds)
    }
}
