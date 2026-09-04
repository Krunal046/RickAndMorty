package com.example.rickandmorty.feature.episode.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.episode.data.episodeDto
import com.example.rickandmorty.feature.episode.data.episodeJson
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.mapper.toEntity
import com.example.rickandmorty.feature.episode.data.remote.EpisodeApiService
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeDTO
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeInfoDTO
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class EpisodeRepositoryImplTest {

    private class FakeApi : EpisodeApiService {
        var response: String = "[]"
        var detail: EpisodeDTO = episodeDto(id = 1, name = "Pilot")
        var failure: Throwable? = null
        val requestedPaths = mutableListOf<String>()

        override suspend fun getEpisodeList(
            page: Int,
            name: String?,
            episode: String?
        ): EpisodeInfoDTO = error("not used")

        override suspend fun getEpisodeById(id: Int): EpisodeDTO {
            failure?.let { throw it }
            return detail
        }

        override suspend fun getEpisodesByIds(ids: String): JsonElement {
            requestedPaths += ids
            failure?.let { throw it }
            return Json.parseToJsonElement(response)
        }
    }

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: EpisodeDao
    private lateinit var api: FakeApi
    private lateinit var repository: EpisodeRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        dao = database.episodeDao()
        api = FakeApi()
        repository = EpisodeRepositoryImpl(
            episodeApi = api,
            episodeDao = dao,
            database = database,
            json = Json { ignoreUnknownKeys = true }
        )
    }

    @After
    fun tearDown() = database.close()

    /**
     * `episode/` with no ids is the paged list endpoint, so a character with no episodes
     * must not reach the network - it would quietly download page one of every episode.
     */
    @Test
    fun `no ids means no request at all`() = runTest {
        val result = repository.refreshEpisodes(emptyList())

        assertTrue(result is Resource.Success)
        assertTrue(api.requestedPaths.isEmpty())
    }

    @Test
    fun `joins the ids into one batch path`() = runTest {
        repository.refreshEpisodes(listOf(1, 2, 3))

        assertEquals(listOf("1,2,3"), api.requestedPaths)
    }

    /** Spec X5, end to end: one id comes back as an object, and still gets cached. */
    @Test
    fun `caches a single episode returned as an object`() = runTest {
        api.response = episodeJson(id = 1, name = "Pilot")

        repository.refreshEpisodes(listOf(1))

        assertEquals(
            listOf("Pilot"),
            repository.observeEpisodes(listOf(1)).first().map(EpisodeModel::name)
        )
    }

    @Test
    fun `caches several episodes returned as an array`() = runTest {
        api.response = "[${episodeJson(id = 1)},${episodeJson(id = 2, name = "Lawnmower Dog")}]"

        repository.refreshEpisodes(listOf(1, 2))

        assertEquals(
            listOf("Pilot", "Lawnmower Dog"),
            repository.observeEpisodes(listOf(1, 2)).first().map(EpisodeModel::name)
        )
        assertEquals(2, dao.countForQuery(EpisodeQuery.BY_ID))
    }

    @Test
    fun `a failed batch leaves the cached episodes on screen`() = runTest {
        api.response = episodeJson(id = 1, name = "Pilot")
        repository.refreshEpisodes(listOf(1))

        api.failure = IOException("offline")
        val result = repository.refreshEpisodes(listOf(1))

        assertTrue(result is Resource.Error)
        assertEquals(1, repository.observeEpisodes(listOf(1)).first().size)
    }

    @Test
    fun `observing no ids emits an empty list without hitting the database`() = runTest {
        assertTrue(repository.observeEpisodes(emptyList()).first().isEmpty())
    }

    /**
     * Spec E2, and the subtle half of it. An episode has one row per list that loaded it,
     * each carrying that list's position, so a detail refresh has to rewrite all of them:
     * writing a single row would either reorder a list or leave the copy `observeById`
     * happens to return stale.
     */
    @Test
    fun `a detail refresh rewrites every cached copy in place`() = runTest {
        dao.upsertAll(
            listOf(
                episodeDto(id = 1, name = "Stale").toEntity(EpisodeQuery.RESOURCE, orderInQuery = 4),
                episodeDto(id = 1, name = "Stale").toEntity("episode:name=pilot", orderInQuery = 0)
            )
        )
        api.detail = episodeDto(id = 1, name = "Pilot")

        repository.refreshEpisode(1)

        val rows = dao.rowsForId(1)
        // Every copy carries the fresh name...
        assertEquals(listOf("Pilot", "Pilot", "Pilot"), rows.map { it.name })
        // ...and none of them lost its place in the list it belongs to.
        assertEquals(
            4,
            rows.first { it.pageQuery == EpisodeQuery.RESOURCE }.orderInQuery
        )
        assertEquals(
            0,
            rows.first { it.pageQuery == "episode:name=pilot" }.orderInQuery
        )
    }

    /**
     * The detail row exists so an episode reached from a character's chips still opens after
     * the search that cached it has been evicted.
     */
    @Test
    fun `a detail refresh caches an episode no list has seen`() = runTest {
        api.detail = episodeDto(id = 1, name = "Pilot")

        repository.refreshEpisode(1)

        assertEquals("Pilot", repository.observeEpisode(1).first()?.name)
        assertEquals(1, dao.countForQuery(EpisodeQuery.DETAIL))
    }

    /** Refreshing twice must not leave two detail rows behind. */
    @Test
    fun `refreshing the detail twice keeps one detail row`() = runTest {
        api.detail = episodeDto(id = 1, name = "Pilot")

        repository.refreshEpisode(1)
        repository.refreshEpisode(1)

        assertEquals(1, dao.countForQuery(EpisodeQuery.DETAIL))
    }

    /** The offline rule: a failed refresh leaves whatever was cached readable. */
    @Test
    fun `a failed detail refresh leaves the cached episode on screen`() = runTest {
        api.detail = episodeDto(id = 1, name = "Pilot")
        repository.refreshEpisode(1)

        api.failure = IOException("offline")
        val result = repository.refreshEpisode(1)

        assertTrue(result is Resource.Error)
        assertEquals("Pilot", repository.observeEpisode(1).first()?.name)
    }
}
