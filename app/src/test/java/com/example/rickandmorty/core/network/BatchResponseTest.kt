package com.example.rickandmorty.core.network

import com.example.rickandmorty.feature.episode.data.episodeJson
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeDTO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Spec X5. The whole point of the helper is the asymmetry between one id and several, so
 * both shapes are pinned here.
 */
class BatchResponseTest {

    private val json = Json { ignoreUnknownKeys = true }

    /** The case a `List<EpisodeDTO>` return type would have failed on. */
    @Test
    fun `a single id answers with an object and still decodes to a list`() {
        val payload = json.parseToJsonElement(episodeJson(id = 7, name = "Raising Gazorpazorp"))

        val episodes = json.decodeBatch<EpisodeDTO>(payload)

        assertEquals(1, episodes.size)
        assertEquals(7, episodes.first().id)
        assertEquals("Raising Gazorpazorp", episodes.first().name)
    }

    @Test
    fun `several ids answer with an array`() {
        val payload = json.parseToJsonElement(
            "[${episodeJson(id = 1)},${episodeJson(id = 2, name = "Lawnmower Dog")}]"
        )

        val episodes = json.decodeBatch<EpisodeDTO>(payload)

        assertEquals(listOf(1, 2), episodes.map(EpisodeDTO::id))
    }

    @Test
    fun `an empty array decodes to no episodes rather than one malformed one`() {
        val episodes = json.decodeBatch<EpisodeDTO>(json.parseToJsonElement("[]"))

        assertTrue(episodes.isEmpty())
    }
}
