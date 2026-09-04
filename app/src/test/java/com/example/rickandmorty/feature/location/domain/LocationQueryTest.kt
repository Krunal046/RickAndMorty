package com.example.rickandmorty.feature.location.domain

import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The key is the identity of a cached list, so anything that gets it wrong either caches the
 * same result set twice or serves one query's rows for another.
 */
class LocationQueryTest {

    @Test
    fun `an empty query caches under the plain resource key`() {
        assertEquals("location", LocationQuery().cacheKey)
        assertTrue(LocationQuery().isEmpty)
    }

    @Test
    fun `filters combine into one key`() {
        val query = LocationQuery(name = "earth", type = "Planet", dimension = "Dimension C-137")

        assertEquals(
            "location:name=earth&type=planet&dimension=dimension c-137",
            query.cacheKey
        )
    }

    /**
     * Two searches that mean the same thing must produce the same key - the API matches
     * case-insensitively, so `Earth` and `earth` are one query, not two.
     */
    @Test
    fun `case and surrounding space do not change the key`() {
        assertEquals(
            LocationQuery(name = "Earth").cacheKey,
            LocationQuery(name = "  earth  ").cacheKey
        )
    }

    /** The parameters are written in a fixed order, or the same filters could cache twice. */
    @Test
    fun `the parameter order does not depend on the order they were set`() {
        val a = LocationQuery(type = "Planet").copy(name = "earth")
        val b = LocationQuery(name = "earth").copy(type = "Planet")

        assertEquals(a.cacheKey, b.cacheKey)
    }

    @Test
    fun `blank filters are dropped rather than sent as empty parameters`() {
        val query = LocationQuery(name = "  ", type = "Planet", dimension = "")

        assertNull(query.nameOrNull())
        assertNull(query.dimensionOrNull())
        assertEquals("Planet", query.typeOrNull())
        assertEquals("location:type=planet", query.cacheKey)
    }

    /** The value sent to the API keeps its original casing; only the key is normalised. */
    @Test
    fun `the api value is not lowercased`() {
        assertEquals("Planet", LocationQuery(type = "Planet").typeOrNull())
    }

    /** The badge counts filters, and the search box is not one. */
    @Test
    fun `the search text is not counted as a filter`() {
        assertEquals(0, LocationQuery(name = "earth").activeFilterCount)
        assertEquals(1, LocationQuery(type = "Planet").activeFilterCount)
        assertEquals(2, LocationQuery(type = "Planet", dimension = "unknown").activeFilterCount)
    }

    /**
     * Every filtered key starts with `location:`, which is what lets
     * `RemoteKeyDao.staleFilteredKeys` find them - and what keeps it away from the plain
     * list, which has no colon.
     */
    @Test
    fun `filtered keys are namespaced but the plain list is not`() {
        assertTrue(LocationQuery(type = "Planet").cacheKey.startsWith("location:"))
        assertFalse(LocationQuery().cacheKey.contains(":"))
    }

    /**
     * The detail key must be unreachable by that eviction, or a character's origin link
     * would break as soon as ten filters had been used.
     */
    @Test
    fun `the detail key is namespaced under the resource`() {
        assertEquals("location:detail", LocationQuery.DETAIL)
    }
}
