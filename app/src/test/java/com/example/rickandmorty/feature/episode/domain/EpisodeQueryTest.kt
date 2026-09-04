package com.example.rickandmorty.feature.episode.domain

import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The screen has one search box and the API has two parameters for it, so which one a piece
 * of text becomes is decided here - and getting it wrong means searching titles for `S01E01`
 * and finding nothing.
 */
class EpisodeQueryTest {

    @Test
    fun `an empty query caches under the plain resource key`() {
        assertEquals("episode", EpisodeQuery().cacheKey)
        assertTrue(EpisodeQuery("   ").isEmpty)
    }

    @Test
    fun `a title goes to the name parameter`() {
        val query = EpisodeQuery("Pilot")

        assertFalse(query.isCodeSearch)
        assertEquals("Pilot", query.nameOrNull())
        assertNull(query.codeOrNull())
        assertEquals("episode:name=pilot", query.cacheKey)
    }

    @Test
    fun `a full episode code goes to the episode parameter`() {
        val query = EpisodeQuery("S01E01")

        assertTrue(query.isCodeSearch)
        assertEquals("S01E01", query.codeOrNull())
        assertNull(query.nameOrNull())
        assertEquals("episode:episode=s01e01", query.cacheKey)
    }

    /** The API matches a code by prefix, so a season on its own is a legitimate search. */
    @Test
    fun `a season on its own is still a code`() {
        assertTrue(EpisodeQuery("S02").isCodeSearch)
        assertEquals("episode:episode=s02", EpisodeQuery("S02").cacheKey)
    }

    @Test
    fun `the code test does not care about case`() {
        assertTrue(EpisodeQuery("s3e7").isCodeSearch)
    }

    /**
     * The heuristic is "S then a digit", not "starts with S" - otherwise every title
     * beginning with an S would be searched as a code and match nothing.
     */
    @Test
    fun `a title starting with S is a title, not a code`() {
        val query = EpisodeQuery("Something Ricked This Way Comes")

        assertFalse(query.isCodeSearch)
        assertEquals("Something Ricked This Way Comes", query.nameOrNull())
    }

    /**
     * Two searches that mean the same thing have to produce the same key or the same result
     * set would be cached twice under different names.
     */
    @Test
    fun `case and surrounding space do not change the cache key`() {
        assertEquals(EpisodeQuery("Pilot").cacheKey, EpisodeQuery("  pilot  ").cacheKey)
        assertEquals(EpisodeQuery("S01E01").cacheKey, EpisodeQuery(" s01e01 ").cacheKey)
    }

    /**
     * `name=s01` and `episode=s01` are different questions with different answers, so the
     * parameter has to be part of the identity of the cached list.
     */
    @Test
    fun `the parameter is part of the key`() {
        assertEquals("episode:episode=s01", EpisodeQuery("S01").cacheKey)
        assertEquals("episode:name=pilot", EpisodeQuery("Pilot").cacheKey)
    }

    /** The value sent to the API keeps its original casing; only the key is normalised. */
    @Test
    fun `the api value is not lowercased`() {
        assertEquals("Pilot", EpisodeQuery("Pilot").nameOrNull())
    }

    /**
     * Every search key starts with `episode:`, which is what makes the eviction in
     * `RemoteKeyDao.staleFilteredKeys` able to find them - and what keeps it away from the
     * plain list, which has no colon.
     */
    @Test
    fun `search keys are namespaced under the resource but the plain list is not`() {
        assertTrue(EpisodeQuery("Pilot").cacheKey.startsWith("episode:"))
        assertFalse(EpisodeQuery().cacheKey.contains(":"))
    }
}
