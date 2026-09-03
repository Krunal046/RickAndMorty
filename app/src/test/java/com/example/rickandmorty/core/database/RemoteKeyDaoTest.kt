package com.example.rickandmorty.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RemoteKeyDaoTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: RemoteKeyDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        dao = database.remoteKeyDao()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `stores and reads back a cursor`() = runTest {
        dao.upsert(RemoteKeyEntity(queryKey = "character", nextPage = 2, lastUpdated = 100L))

        assertEquals(2, dao.remoteKey("character")?.nextPage)
    }

    @Test
    fun `upsert replaces the cursor for the same query`() = runTest {
        dao.upsert(RemoteKeyEntity(queryKey = "character", nextPage = 2, lastUpdated = 100L))
        dao.upsert(RemoteKeyEntity(queryKey = "character", nextPage = 3, lastUpdated = 200L))

        assertEquals(3, dao.remoteKey("character")?.nextPage)
    }

    /**
     * The reason the table is keyed by query rather than by resource: a filtered list pages
     * independently of the unfiltered one, and clearing one must not disturb the other.
     */
    @Test
    fun `cursors for different queries of the same resource are independent`() = runTest {
        dao.upsert(RemoteKeyEntity(queryKey = "character", nextPage = 5, lastUpdated = 100L))
        dao.upsert(
            RemoteKeyEntity(queryKey = "character:status=alive", nextPage = 2, lastUpdated = 100L)
        )

        dao.clear("character:status=alive")

        assertEquals(5, dao.remoteKey("character")?.nextPage)
        assertNull(dao.remoteKey("character:status=alive"))
    }

    @Test
    fun `a null next page marks the end of the list`() = runTest {
        dao.upsert(RemoteKeyEntity(queryKey = "episode", nextPage = null, lastUpdated = 100L))

        val key = dao.remoteKey("episode")

        assertNull(key?.nextPage)
        assertEquals(100L, key?.lastUpdated)
    }
}
