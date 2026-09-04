package com.example.rickandmorty.feature.episode.data.paging

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
import com.example.rickandmorty.feature.episode.data.episodeDto
import com.example.rickandmorty.feature.episode.data.episodePage
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import com.example.rickandmorty.feature.episode.data.mapper.toEntity
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeInfoDTO
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
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
 * The episode list is the character list's rules applied to the other resource, so this
 * covers the same ground `CharacterRemoteMediatorTest` does - a copied mediator that quietly
 * dropped one of those rules would otherwise pass everything.
 */
@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class EpisodeRemoteMediatorTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: EpisodeDao

    private val query = EpisodeQuery.RESOURCE

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        dao = database.episodeDao()
    }

    @After
    fun tearDown() = database.close()

    private fun mediator(
        pageQuery: String = query,
        fetchPage: suspend (Int) -> EpisodeInfoDTO
    ) = EpisodeRemoteMediator(
        pageQuery = pageQuery,
        database = database,
        episodeDao = dao,
        fetchPage = fetchPage
    )

    private fun emptyState() = PagingState<Int, EpisodeEntity>(
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
            episodePage(
                episodes = listOf(episodeDto(id = 1), episodeDto(id = 2)),
                next = "https://rickandmortyapi.com/api/episode?page=2"
            )
        }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(2, dao.countForQuery(query))
    }

    /** info.next, not the 3 pages the API reports, is what ends the list. */
    @Test
    fun `a null next marks the end of pagination`() = runTest {
        val mediator = mediator { episodePage(listOf(episodeDto(id = 1)), next = null) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `append continues from the stored cursor and keeps the earlier rows`() = runTest {
        val requested = mutableListOf<Int>()
        val mediator = mediator { page ->
            requested += page
            episodePage(
                episodes = listOf(episodeDto(id = page * 10)),
                next = "https://rickandmortyapi.com/api/episode?page=${page + 1}"
            )
        }

        mediator.load(LoadType.REFRESH, emptyState())
        mediator.load(LoadType.APPEND, emptyState())

        assertEquals(listOf(1, 2), requested)
        assertEquals(2, dao.countForQuery(query))
        // The appended page continues the ordering rather than restarting it, which is what
        // keeps the seasons in airing order across a page boundary.
        assertEquals(1, dao.maxOrder(query))
    }

    @Test
    fun `append stops once the cursor is exhausted, without calling the api again`() = runTest {
        var calls = 0
        val mediator = mediator { page ->
            calls++
            episodePage(listOf(episodeDto(id = page)), next = null)
        }

        mediator.load(LoadType.REFRESH, emptyState())
        val result = mediator.load(LoadType.APPEND, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(1, calls)
    }

    @Test
    fun `refresh replaces the previous rows rather than accumulating them`() = runTest {
        val mediator = mediator { episodePage(listOf(episodeDto(id = 1)), next = null) }

        mediator.load(LoadType.REFRESH, emptyState())
        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(1, dao.countForQuery(query))
        assertEquals(0, dao.maxOrder(query))
    }

    @Test
    fun `prepend is a no-op because the api only pages forward`() = runTest {
        var calls = 0
        val mediator = mediator { calls++; episodePage(emptyList(), next = null) }

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
            episodePage(listOf(episodeDto(id = 1), episodeDto(id = 2)), next = null)
        }
        val searched = mediator(pageQuery = "episode:name=pilot") {
            episodePage(listOf(episodeDto(id = 1)), next = null)
        }

        plain.load(LoadType.REFRESH, emptyState())
        searched.load(LoadType.REFRESH, emptyState())
        searched.load(LoadType.REFRESH, emptyState())

        assertEquals(2, dao.countForQuery(query))
        assertEquals(1, dao.countForQuery("episode:name=pilot"))
    }

    @Test
    fun `a network failure surfaces as a typed DataError, not a raw exception`() = runTest {
        val mediator = mediator { throw IOException("offline") }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        val error = (result as RemoteMediator.MediatorResult.Error).throwable
        assertTrue(error is DataErrorException)
        assertEquals(DataError.NoInternet, (error as DataErrorException).error)
    }

    /**
     * Spec §8: a search with no matches answers 404, not an empty list. That is an empty
     * state, not a failure - a retry button in front of a user whose search simply had no
     * hits would be wrong.
     */
    @Test
    fun `a 404 on a search is an empty result, not an error`() = runTest {
        val mediator = mediator(pageQuery = "episode:name=zzzzzz") { throw httpException(404) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, dao.countForQuery("episode:name=zzzzzz"))
    }

    /** A search that stops matching must clear what the previous one left behind. */
    @Test
    fun `a 404 refresh clears the rows the previous search cached`() = runTest {
        val searchKey = "episode:name=pilot"
        mediator(pageQuery = searchKey) {
            episodePage(listOf(episodeDto(id = 1)), next = null)
        }.load(LoadType.REFRESH, emptyState())

        mediator(pageQuery = searchKey) { throw httpException(404) }
            .load(LoadType.REFRESH, emptyState())

        assertEquals(0, dao.countForQuery(searchKey))
    }

    /** A 500 is a real failure and must not be mistaken for an empty search. */
    @Test
    fun `a server error is still an error`() = runTest {
        val mediator = mediator { throw httpException(500) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Error)
    }

    /**
     * Every distinct search caches under its own key, so without a bound the table would
     * grow for the lifetime of the install.
     */
    @Test
    fun `a refresh evicts the least recently used searches`() = runTest {
        repeat(11) { index ->
            database.remoteKeyDao().upsert(
                RemoteKeyEntity(
                    queryKey = "episode:name=search$index",
                    nextPage = null,
                    lastUpdated = index.toLong()
                )
            )
            dao.upsertAll(
                listOf(
                    episodeDto(id = index).toEntity("episode:name=search$index", orderInQuery = 0)
                )
            )
        }

        mediator { episodePage(listOf(episodeDto(id = 100)), next = null) }
            .load(LoadType.REFRESH, emptyState())

        // The oldest search is gone; the newest is untouched.
        assertEquals(0, dao.countForQuery("episode:name=search0"))
        assertEquals(1, dao.countForQuery("episode:name=search10"))
    }

    /** The plain list is the default screen and is never evicted. */
    @Test
    fun `the unfiltered list is never evicted`() = runTest {
        repeat(12) { index ->
            database.remoteKeyDao().upsert(
                RemoteKeyEntity(
                    queryKey = "episode:name=search$index",
                    nextPage = null,
                    lastUpdated = index.toLong()
                )
            )
        }

        mediator { episodePage(listOf(episodeDto(id = 1)), next = null) }
            .load(LoadType.REFRESH, emptyState())

        assertEquals(1, dao.countForQuery(query))
    }
}
