package com.example.rickandmorty.feature.location.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.location.data.local.dao.LocationDao
import com.example.rickandmorty.feature.location.data.locationDto
import com.example.rickandmorty.feature.location.data.mapper.toEntity
import com.example.rickandmorty.feature.location.data.remote.LocationApiService
import com.example.rickandmorty.feature.location.data.remote.dto.LocationDTO
import com.example.rickandmorty.feature.location.data.remote.dto.LocationInfoDTO
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

/**
 * The detail cache is the subtle part of spec L2, and it carries more weight here than on
 * the other two screens: a character's origin link (spec C3) opens a location that no
 * location list has necessarily ever loaded.
 */
@RunWith(RobolectricTestRunner::class)
class LocationRepositoryImplTest {

    private class FakeApi : LocationApiService {
        var detail: LocationDTO = locationDto(id = 1, name = "Earth (C-137)")
        var failure: Throwable? = null
        val requestedIds = mutableListOf<Int>()

        override suspend fun getLocationList(
            page: Int,
            name: String?,
            type: String?,
            dimension: String?
        ): LocationInfoDTO = error("not used")

        override suspend fun getLocationById(id: Int): LocationDTO {
            requestedIds += id
            failure?.let { throw it }
            return detail
        }
    }

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: LocationDao
    private lateinit var api: FakeApi
    private lateinit var repository: LocationRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        dao = database.locationDao()
        api = FakeApi()
        repository = LocationRepositoryImpl(
            locationApi = api,
            locationDao = dao,
            database = database
        )
    }

    @After
    fun tearDown() = database.close()

    /**
     * The case that makes C3's origin link work: nothing has been listed, so the detail row
     * is the only copy there is.
     */
    @Test
    fun `a detail refresh caches a location no list has seen`() = runTest {
        repository.refreshLocation(1)

        assertEquals("Earth (C-137)", repository.observeLocation(1).first()?.name)
        assertEquals(1, dao.countForQuery(LocationQuery.DETAIL))
    }

    /**
     * A location has one row per list that loaded it, each carrying that list's position, so
     * a refresh has to rewrite all of them: writing a single row would either reorder a list
     * or leave the copy `observeById` happens to return stale.
     */
    @Test
    fun `a detail refresh rewrites every cached copy in place`() = runTest {
        dao.upsertAll(
            listOf(
                locationDto(id = 1, name = "Stale").toEntity(LocationQuery.RESOURCE, 4),
                locationDto(id = 1, name = "Stale").toEntity("location:type=planet", 0)
            )
        )
        api.detail = locationDto(id = 1, name = "Earth (C-137)")

        repository.refreshLocation(1)

        val rows = dao.rowsForId(1)
        // Every copy carries the fresh name...
        assertEquals(listOf("Earth (C-137)"), rows.map { it.name }.distinct())
        // ...and none of them lost its place in the list it belongs to.
        assertEquals(4, rows.first { it.pageQuery == LocationQuery.RESOURCE }.orderInQuery)
        assertEquals(0, rows.first { it.pageQuery == "location:type=planet" }.orderInQuery)
    }

    @Test
    fun `refreshing the detail twice keeps one detail row`() = runTest {
        repository.refreshLocation(1)
        repository.refreshLocation(1)

        assertEquals(1, dao.countForQuery(LocationQuery.DETAIL))
    }

    /** The offline rule: a failed refresh leaves whatever was cached readable. */
    @Test
    fun `a failed refresh leaves the cached location on screen`() = runTest {
        repository.refreshLocation(1)

        api.failure = IOException("offline")
        val result = repository.refreshLocation(1)

        assertTrue(result is Resource.Error)
        assertEquals("Earth (C-137)", repository.observeLocation(1).first()?.name)
    }

    @Test
    fun `a failure is reported as a typed error rather than thrown`() = runTest {
        api.failure = IOException("offline")

        val result = repository.refreshLocation(1)

        assertTrue(result is Resource.Error)
    }

    @Test
    fun `nothing is emitted for a location that was never cached`() = runTest {
        assertEquals(null, repository.observeLocation(99).first())
    }
}
