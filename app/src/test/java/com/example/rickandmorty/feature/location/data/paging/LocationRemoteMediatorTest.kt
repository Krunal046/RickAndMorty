package com.example.rickandmorty.feature.location.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.core.common.DataErrorException
import com.example.rickandmorty.core.database.RemoteKeyEntity
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.location.data.local.dao.LocationDao
import com.example.rickandmorty.feature.location.data.local.entity.LocationEntity
import com.example.rickandmorty.feature.location.data.locationDto
import com.example.rickandmorty.feature.location.data.locationPage
import com.example.rickandmorty.feature.location.data.mapper.toEntity
import com.example.rickandmorty.feature.location.data.remote.dto.LocationInfoDTO
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.HttpException
import java.io.IOException

/**
 * The third mediator built from the same rules, so it is held to the same behaviour: a copy
 * that quietly dropped one of them would otherwise pass everything else in the suite.
 */
@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationRemoteMediatorTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: LocationDao

    private val query = LocationQuery.RESOURCE

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

    private fun mediator(
        pageQuery: String = query,
        fetchPage: suspend (Int) -> LocationInfoDTO
    ) = LocationRemoteMediator(
        pageQuery = pageQuery,
        database = database,
        locationDao = dao,
        fetchPage = fetchPage
    )

    private fun emptyState() = PagingState<Int, LocationEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    private fun httpException(code: Int) = HttpException(
        retrofit2.Response.error<Any>(
            code,
            """{"error":"There is nothing here"}""".toResponseBody("application/json".toMediaType())
        )
    )

    @Test
    fun `refresh writes the first page into the database`() = runTest {
        val mediator = mediator {
            locationPage(
                locations = listOf(locationDto(id = 1), locationDto(id = 2)),
                next = "https://rickandmortyapi.com/api/location?page=2"
            )
        }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(2, dao.countForQuery(query))
    }

    /** info.next, not the 7 pages the API reports, is what ends the list. */
    @Test
    fun `a null next marks the end of pagination`() = runTest {
        val mediator = mediator { locationPage(listOf(locationDto(id = 1)), next = null) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `append continues from the stored cursor and keeps the earlier rows`() = runTest {
        val requested = mutableListOf<Int>()
        val mediator = mediator { page ->
            requested += page
            locationPage(
                locations = listOf(locationDto(id = page * 10)),
                next = "https://rickandmortyapi.com/api/location?page=${page + 1}"
            )
        }

        mediator.load(LoadType.REFRESH, emptyState())
        mediator.load(LoadType.APPEND, emptyState())

        assertEquals(listOf(1, 2), requested)
        assertEquals(2, dao.countForQuery(query))
        assertEquals(1, dao.maxOrder(query))
    }

    @Test
    fun `append stops once the cursor is exhausted, without calling the api again`() = runTest {
        var calls = 0
        val mediator = mediator { page ->
            calls++
            locationPage(listOf(locationDto(id = page)), next = null)
        }

        mediator.load(LoadType.REFRESH, emptyState())
        val result = mediator.load(LoadType.APPEND, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(1, calls)
    }

    @Test
    fun `refresh replaces the previous rows rather than accumulating them`() = runTest {
        val mediator = mediator { locationPage(listOf(locationDto(id = 1)), next = null) }

        mediator.load(LoadType.REFRESH, emptyState())
        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(1, dao.countForQuery(query))
        assertEquals(0, dao.maxOrder(query))
    }

    @Test
    fun `prepend is a no-op because the api only pages forward`() = runTest {
        var calls = 0
        val mediator = mediator { calls++; locationPage(emptyList(), next = null) }

        val result = mediator.load(LoadType.PREPEND, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, calls)
    }

    /**
     * The point of keying on the query: refreshing one list must not disturb another's rows
     * or its cursor.
     */
    @Test
    fun `refreshing one query leaves another query's cache intact`() = runTest {
        val plain = mediator(pageQuery = query) {
            locationPage(listOf(locationDto(id = 1), locationDto(id = 2)), next = null)
        }
        val filtered = mediator(pageQuery = "location:type=planet") {
            locationPage(listOf(locationDto(id = 1)), next = null)
        }

        plain.load(LoadType.REFRESH, emptyState())
        filtered.load(LoadType.REFRESH, emptyState())
        filtered.load(LoadType.REFRESH, emptyState())

        assertEquals(2, dao.countForQuery(query))
        assertEquals(1, dao.countForQuery("location:type=planet"))
    }

    @Test
    fun `a network failure surfaces as a typed DataError, not a raw exception`() = runTest {
        val mediator = mediator { throw IOException("offline") }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        val error = (result as RemoteMediator.MediatorResult.Error).throwable
        assertTrue(error is DataErrorException)
        assertEquals(DataError.NoInternet, (error as DataErrorException).error)
    }

    /** Spec §8: a filter that matches nothing answers 404, and that is an empty state. */
    @Test
    fun `a 404 on a filtered search is an empty result, not an error`() = runTest {
        val mediator = mediator(pageQuery = "location:name=zzzzzz") { throw httpException(404) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, dao.countForQuery("location:name=zzzzzz"))
    }

    /** A filter that stops matching must clear what the previous one left behind. */
    @Test
    fun `a 404 refresh clears the rows the previous filter cached`() = runTest {
        val filterKey = "location:type=planet"
        mediator(pageQuery = filterKey) {
            locationPage(listOf(locationDto(id = 1)), next = null)
        }.load(LoadType.REFRESH, emptyState())

        mediator(pageQuery = filterKey) { throw httpException(404) }
            .load(LoadType.REFRESH, emptyState())

        assertEquals(0, dao.countForQuery(filterKey))
    }

    /** A 500 is a real failure and must not be mistaken for an empty search. */
    @Test
    fun `a server error is still an error`() = runTest {
        val mediator = mediator { throw httpException(500) }

        assertTrue(
            mediator.load(LoadType.REFRESH, emptyState()) is RemoteMediator.MediatorResult.Error
        )
    }

    @Test
    fun `a refresh evicts the least recently used filters`() = runTest {
        repeat(11) { index ->
            database.remoteKeyDao().upsert(
                RemoteKeyEntity(
                    queryKey = "location:name=search$index",
                    nextPage = null,
                    lastUpdated = index.toLong()
                )
            )
            dao.upsertAll(
                listOf(locationDto(id = index).toEntity("location:name=search$index", 0))
            )
        }

        mediator { locationPage(listOf(locationDto(id = 100)), next = null) }
            .load(LoadType.REFRESH, emptyState())

        assertEquals(0, dao.countForQuery("location:name=search0"))
        assertEquals(1, dao.countForQuery("location:name=search10"))
    }

    /**
     * The detail row is what a character's origin link opens, and it is never written to
     * `remote_keys` - so no amount of filtering can evict it. Without this the link would
     * break after ten searches.
     */
    @Test
    fun `the eviction cannot reach a cached detail`() = runTest {
        dao.upsertAll(listOf(locationDto(id = 42).toEntity(LocationQuery.DETAIL, 0)))
        repeat(12) { index ->
            database.remoteKeyDao().upsert(
                RemoteKeyEntity(
                    queryKey = "location:name=search$index",
                    nextPage = null,
                    lastUpdated = index.toLong()
                )
            )
        }

        mediator { locationPage(listOf(locationDto(id = 1)), next = null) }
            .load(LoadType.REFRESH, emptyState())

        assertEquals(1, dao.countForQuery(LocationQuery.DETAIL))
    }
}
