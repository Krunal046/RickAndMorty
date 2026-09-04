package com.example.rickandmorty.feature.episode.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.episode.data.episodeJson
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import com.example.rickandmorty.feature.episode.data.remote.EpisodeApiService
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
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
        var failure: Throwable? = null
        val requestedPaths = mutableListOf<String>()

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
        assertEquals(2, dao.countForQuery(EpisodeEntity.BY_ID_QUERY))
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
}
