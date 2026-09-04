package com.example.rickandmorty.feature.location.data.local

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.location.data.local.dao.LocationDao
import com.example.rickandmorty.feature.location.data.locationDto
import com.example.rickandmorty.feature.location.data.mapper.toEntity
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LocationDaoTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: LocationDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        dao = database.locationDao()
    }

    @After
    fun tearDown() = database.close()

    private fun rows(query: String, ids: List<Int>) = ids.mapIndexed { index, id ->
        locationDto(id = id, name = "Location $id")
            .toEntity(pageQuery = query, orderInQuery = index)
    }

    /**
     * Paging reads rows in the order they arrived, not by id: sorting by id would reorder a
     * filtered list, and SQLite gives no guarantee of insertion order without a sort.
     */
    @Test
    fun `the paging source is scoped to one query and ordered by position`() = runTest {
        dao.upsertAll(rows(LocationQuery.RESOURCE, listOf(3, 1, 2)))
        dao.upsertAll(rows("location:type=planet", listOf(9)))

        val page = dao.pagingSource(LocationQuery.RESOURCE).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(listOf(3, 1, 2), page.data.map { it.id })
    }

    /** Whichever list happened to cache it will do for the detail screen. */
    @Test
    fun `the detail reads any cached copy of a location`() = runTest {
        dao.upsertAll(rows("location:type=planet", listOf(7)))

        assertEquals(7, dao.observeById(7).first()?.id)
    }

    @Test
    fun `the detail emits null while nothing is cached`() = runTest {
        assertNull(dao.observeById(7).first())
    }

    /**
     * A detail refresh has to rewrite every copy together, so it first has to find them all -
     * the copy `observeById` picks is otherwise free to be the stale one.
     */
    @Test
    fun `every cached copy of a location is reachable for a rewrite`() = runTest {
        dao.upsertAll(rows(LocationQuery.RESOURCE, listOf(1)))
        dao.upsertAll(rows("location:type=planet", listOf(1)))
        dao.upsertAll(rows(LocationQuery.DETAIL, listOf(1)))

        assertEquals(
            listOf(LocationQuery.RESOURCE, LocationQuery.DETAIL, "location:type=planet"),
            dao.rowsForId(1).map { it.pageQuery }.sorted()
        )
    }

    /**
     * The same location legitimately appears in several lists at once, and clearing one must
     * not take the others' rows with it.
     */
    @Test
    fun `clearing one query leaves the same location cached under another`() = runTest {
        dao.upsertAll(rows(LocationQuery.RESOURCE, listOf(1)))
        dao.upsertAll(rows("location:type=planet", listOf(1)))

        dao.clearForQuery("location:type=planet")

        assertEquals(1, dao.countForQuery(LocationQuery.RESOURCE))
        assertEquals(0, dao.countForQuery("location:type=planet"))
    }

    @Test
    fun `re-fetching the same page replaces the rows rather than duplicating them`() = runTest {
        dao.upsertAll(rows(LocationQuery.RESOURCE, listOf(1, 2)))
        dao.upsertAll(rows(LocationQuery.RESOURCE, listOf(1, 2)))

        assertEquals(2, dao.countForQuery(LocationQuery.RESOURCE))
    }
}
