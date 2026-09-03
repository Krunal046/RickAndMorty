package com.example.rickandmorty.feature.character.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.core.common.DataErrorException
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.character.data.characterDto
import com.example.rickandmorty.feature.character.data.characterPage
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import java.io.IOException

@OptIn(ExperimentalPagingApi::class)
@RunWith(RobolectricTestRunner::class)
class CharacterRemoteMediatorTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: CharacterDao

    private val query = "character"

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        dao = database.characterDao()
    }

    @After
    fun tearDown() = database.close()

    private fun mediator(
        pageQuery: String = query,
        fetchPage: suspend (Int) -> CharacterInfoDTO
    ) = CharacterRemoteMediator(
        pageQuery = pageQuery,
        database = database,
        characterDao = dao,
        fetchPage = fetchPage
    )

    private fun emptyState() = PagingState<Int, CharacterEntity>(
        pages = emptyList(),
        anchorPosition = null,
        config = PagingConfig(pageSize = 20),
        leadingPlaceholderCount = 0
    )

    @Test
    fun `refresh writes the first page into the database`() = runTest {
        val mediator = mediator {
            characterPage(
                characters = listOf(characterDto(id = 1), characterDto(id = 2)),
                next = "https://rickandmortyapi.com/api/character?page=2"
            )
        }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertFalse((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(2, dao.countForQuery(query))
    }

    /** info.next, not a page count, is what ends the list. */
    @Test
    fun `a null next marks the end of pagination`() = runTest {
        val mediator = mediator { characterPage(listOf(characterDto(id = 1)), next = null) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
    }

    @Test
    fun `append continues from the stored cursor and keeps the earlier rows`() = runTest {
        var requested = mutableListOf<Int>()
        val mediator = mediator { page ->
            requested += page
            characterPage(
                characters = listOf(characterDto(id = page * 10)),
                next = "https://rickandmortyapi.com/api/character?page=${page + 1}"
            )
        }

        mediator.load(LoadType.REFRESH, emptyState())
        mediator.load(LoadType.APPEND, emptyState())

        assertEquals(listOf(1, 2), requested)
        assertEquals(2, dao.countForQuery(query))
        // The appended page continues the ordering rather than restarting it.
        assertEquals(1, dao.maxOrder(query))
    }

    @Test
    fun `append stops once the cursor is exhausted, without calling the api again`() = runTest {
        var calls = 0
        val mediator = mediator { page ->
            calls++
            characterPage(listOf(characterDto(id = page)), next = null)
        }

        mediator.load(LoadType.REFRESH, emptyState())
        val result = mediator.load(LoadType.APPEND, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(1, calls)
    }

    @Test
    fun `refresh replaces the previous rows rather than accumulating them`() = runTest {
        val mediator = mediator { characterPage(listOf(characterDto(id = 1)), next = null) }

        mediator.load(LoadType.REFRESH, emptyState())
        mediator.load(LoadType.REFRESH, emptyState())

        assertEquals(1, dao.countForQuery(query))
        assertEquals(0, dao.maxOrder(query))
    }

    /**
     * The point of keying on the query: refreshing one list must not disturb another's rows
     * or its cursor.
     */
    @Test
    fun `refreshing one query leaves another query's cache intact`() = runTest {
        val plain = mediator(pageQuery = "character") {
            characterPage(listOf(characterDto(id = 1), characterDto(id = 2)), next = null)
        }
        val filtered = mediator(pageQuery = "character:name=rick") {
            characterPage(listOf(characterDto(id = 1)), next = null)
        }

        plain.load(LoadType.REFRESH, emptyState())
        filtered.load(LoadType.REFRESH, emptyState())
        filtered.load(LoadType.REFRESH, emptyState())

        assertEquals(2, dao.countForQuery("character"))
        assertEquals(1, dao.countForQuery("character:name=rick"))
    }

    @Test
    fun `prepend is a no-op because the api only pages forward`() = runTest {
        var calls = 0
        val mediator = mediator { calls++; characterPage(emptyList(), next = null) }

        val result = mediator.load(LoadType.PREPEND, emptyState())

        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, calls)
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
     * The offline guarantee: a refresh that fails must leave whatever was cached readable,
     * because the UI renders from the database and has nothing else to fall back on.
     */
    private fun httpException(code: Int) = HttpException(
        retrofit2.Response.error<Any>(
            code,
            """{"error":"There is nothing here"}""".toResponseBody("application/json".toMediaType())
        )
    )

    /**
     * Spec §8: the API answers a search with no matches with 404, not an empty list. That
     * is an empty state, not a failure - a retry button in front of a user whose search
     * simply had no hits would be wrong.
     */
    @Test
    fun `a 404 on a filtered search is an empty result, not an error`() = runTest {
        val mediator = mediator(pageQuery = "character:name=zzzzzz") {
            throw httpException(404)
        }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        assertTrue(result is RemoteMediator.MediatorResult.Success)
        assertTrue((result as RemoteMediator.MediatorResult.Success).endOfPaginationReached)
        assertEquals(0, dao.countForQuery("character:name=zzzzzz"))
    }

    /** A search that stops matching must clear what the previous one left behind. */
    @Test
    fun `a 404 refresh clears the rows the previous search cached`() = runTest {
        val query = "character:name=ric"
        val found = mediator(pageQuery = query) {
            characterPage(listOf(characterDto(id = 1)), next = null)
        }
        found.load(LoadType.REFRESH, emptyState())
        assertEquals(1, dao.countForQuery(query))

        val notFound = mediator(pageQuery = query) { throw httpException(404) }
        notFound.load(LoadType.REFRESH, emptyState())

        assertEquals(0, dao.countForQuery(query))
    }

    /** Only 404 is an empty result; a server fault is still a failure worth retrying. */
    @Test
    fun `a 500 is still an error`() = runTest {
        val mediator = mediator { throw httpException(500) }

        val result = mediator.load(LoadType.REFRESH, emptyState())

        val error = (result as RemoteMediator.MediatorResult.Error).throwable
        assertEquals(DataError.Http(500), (error as DataErrorException).error)
    }

    /**
     * Every distinct search caches under its own key, so without a bound the table would
     * grow for the lifetime of the install.
     */
    @Test
    fun `only the most recent filtered searches are kept`() = runTest {
        repeat(12) { index ->
            mediator(pageQuery = "character:name=search$index") {
                characterPage(listOf(characterDto(id = index + 1)), next = null)
            }.load(LoadType.REFRESH, emptyState())
        }

        val surviving = (0 until 12).count { dao.countForQuery("character:name=search$it") > 0 }

        assertEquals(10, surviving)
        // The oldest went first.
        assertEquals(0, dao.countForQuery("character:name=search0"))
        assertEquals(1, dao.countForQuery("character:name=search11"))
    }

    /** The unfiltered list is the app's default screen and is never evicted. */
    @Test
    fun `the plain list survives the trim`() = runTest {
        mediator(pageQuery = "character") {
            characterPage(listOf(characterDto(id = 1)), next = null)
        }.load(LoadType.REFRESH, emptyState())

        repeat(12) { index ->
            mediator(pageQuery = "character:name=search$index") {
                characterPage(listOf(characterDto(id = index + 100)), next = null)
            }.load(LoadType.REFRESH, emptyState())
        }

        assertEquals(1, dao.countForQuery("character"))
    }

    @Test
    fun `a failed refresh leaves the cached rows in place`() = runTest {
        val ok = mediator { characterPage(listOf(characterDto(id = 1)), next = null) }
        ok.load(LoadType.REFRESH, emptyState())

        val failing = mediator { throw IOException("offline") }
        failing.load(LoadType.REFRESH, emptyState())

        assertEquals(1, dao.countForQuery(query))
        assertEquals("Rick Sanchez", dao.observeById(1).first()?.name)
    }
}
