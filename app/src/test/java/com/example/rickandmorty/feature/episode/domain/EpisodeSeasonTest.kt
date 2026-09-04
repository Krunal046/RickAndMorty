package com.example.rickandmorty.feature.episode.domain

import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The API has no season field, so spec E1's grouping rests entirely on reading the number
 * back out of the `S01E01` code.
 */
class EpisodeSeasonTest {

    private fun episode(code: String) = EpisodeModel(
        id = 1,
        name = "Pilot",
        airDate = "December 2, 2013",
        code = code,
        characterIds = emptyList(),
        url = "",
        created = ""
    )

    @Test
    fun `reads the season out of the code`() {
        assertEquals(1, episode("S01E01").season)
        assertEquals(3, episode("S03E07").season)
    }

    /** Season 10 would be misread by anything that took a single character. */
    @Test
    fun `handles a two digit season`() {
        assertEquals(10, episode("S10E01").season)
    }

    @Test
    fun `is not confused by a lowercase code`() {
        assertEquals(2, episode("s02e05").season)
    }

    /**
     * Null rather than a default: filing a malformed episode under season 1 alongside real
     * ones is a worse answer than admitting the code could not be read.
     */
    @Test
    fun `a code that is not in that shape has no season`() {
        assertNull(episode("").season)
        assertNull(episode("pilot").season)
        assertNull(episode("SxxExx").season)
        assertNull(episode("E01").season)
    }
}
