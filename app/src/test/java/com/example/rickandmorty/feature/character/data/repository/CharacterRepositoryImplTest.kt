package com.example.rickandmorty.feature.character.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.character.data.characterDto
import com.example.rickandmorty.feature.character.data.characterJson
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import com.example.rickandmorty.feature.character.data.mapper.toEntity
import com.example.rickandmorty.feature.character.data.remote.CharacterApiService
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
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

/**
 * The detail cache is the subtle part of spec C3: a character has one row per list that
 * loaded it, each carrying that list's position, so a refresh has to update all of them
 * without disturbing any ordering.
 */
@RunWith(RobolectricTestRunner::class)
class CharacterRepositoryImplTest {

    private class FakeApi : CharacterApiService {
        var character: CharacterDTO = characterDto(id = 1, name = "Rick Sanchez")
        var failure: Throwable? = null
        var batchResponse: String = "[]"
        val batchPaths = mutableListOf<String>()

        override suspend fun getCharacterList(
            page: Int,
            name: String?,
            status: String?,
            species: String?,
            gender: String?
        ): CharacterInfoDTO = error("not used")

        override suspend fun getCharacterById(id: Int): CharacterDTO {
            failure?.let { throw it }
            return character
        }

        override suspend fun getCharactersByIds(ids: String): JsonElement {
            batchPaths += ids
            failure?.let { throw it }
            return Json.parseToJsonElement(batchResponse)
        }
    }

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: CharacterDao
    private lateinit var api: FakeApi
    private lateinit var repository: CharacterRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        dao = database.characterDao()
        api = FakeApi()
        repository = CharacterRepositoryImpl(
            characterApi = api,
            characterDao = dao,
            database = database,
            json = Json { ignoreUnknownKeys = true }
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun `a character never cached by a list is still cached for the detail screen`() = runTest {
        repository.refreshCharacter(1)

        assertEquals(
            listOf(CharacterQuery.DETAIL),
            dao.rowsForId(1).map(CharacterEntity::pageQuery)
        )
        assertEquals("Rick Sanchez", repository.observeCharacter(1).first()?.name)
    }

    /**
     * The reason the refresh rewrites rows in place: the detail knows no list position, so
     * upserting a single row would move the character to the top of every list holding it.
     */
    @Test
    fun `a refresh updates every cached copy without moving it in its list`() = runTest {
        dao.upsertAll(
            listOf(
                characterDto(id = 1, name = "Stale").toEntity("character", orderInQuery = 12),
                characterDto(id = 1, name = "Stale").toEntity("character:name=rick", orderInQuery = 3)
            )
        )
        api.character = characterDto(id = 1, name = "Rick Sanchez")

        repository.refreshCharacter(1)

        val rows = dao.rowsForId(1).associateBy(CharacterEntity::pageQuery)
        assertEquals(12, rows.getValue("character").orderInQuery)
        assertEquals(3, rows.getValue("character:name=rick").orderInQuery)
        assertTrue(rows.values.all { it.name == "Rick Sanchez" })
    }

    /** Every copy is rewritten together, so `observeById` cannot land on a stale one. */
    @Test
    fun `a second refresh does not add another detail row`() = runTest {
        repository.refreshCharacter(1)
        repository.refreshCharacter(1)

        assertEquals(1, dao.rowsForId(1).size)
    }

    @Test
    fun `a failed refresh leaves the cache alone so the screen keeps its content`() = runTest {
        dao.upsertAll(listOf(characterDto(id = 1, name = "Cached").toEntity("character", 0)))
        api.failure = IOException("offline")

        val result = repository.refreshCharacter(1)

        assertTrue(result is Resource.Error)
        assertEquals("Cached", repository.observeCharacter(1).first()?.name)
    }

    /**
     * `character/` with no ids is the paged list endpoint, so an empty batch must not reach
     * the network at all - it would quietly download page one.
     */
    @Test
    fun `an empty batch request never touches the network`() = runTest {
        val result = repository.refreshCharacters(emptyList())

        assertTrue(result is Resource.Success)
        assertTrue(api.batchPaths.isEmpty())
    }

    @Test
    fun `a batch request joins the ids into one path`() = runTest {
        repository.refreshCharacters(listOf(1, 2, 3))

        assertEquals(listOf("1,2,3"), api.batchPaths)
    }

    /**
     * Spec E4 reads its cast from the database like everything else, so the batch has to
     * write what it fetched rather than hand it back to the caller.
     */
    @Test
    fun `a batch request caches what it fetched`() = runTest {
        api.batchResponse = "[${characterJson(id = 1, name = "Rick Sanchez")}]"

        repository.refreshCharacters(listOf(1))

        assertEquals(
            listOf("Rick Sanchez"),
            repository.observeCharactersByIds(listOf(1)).first().map(CharacterModel::name)
        )
        assertEquals(1, dao.countForQuery(CharacterQuery.BY_ID))
    }

    /** The point of caching it: the grid still renders once the network is gone. */
    @Test
    fun `a failed batch leaves the cached cast on screen`() = runTest {
        api.batchResponse = "[${characterJson(id = 1, name = "Rick Sanchez")}]"
        repository.refreshCharacters(listOf(1))

        api.failure = IOException("offline")
        val result = repository.refreshCharacters(listOf(1))

        assertTrue(result is Resource.Error)
        assertEquals(1, repository.observeCharactersByIds(listOf(1)).first().size)
    }

    /**
     * A character cached by a list and by a cast is one character, not two: the copies are
     * per list, and a grid that showed a duplicate would be showing the same person twice.
     */
    @Test
    fun `a character cached by both a list and a batch appears once`() = runTest {
        dao.upsertAll(
            listOf(characterDto(id = 1).toEntity(pageQuery = "character", orderInQuery = 0))
        )
        api.batchResponse = "[${characterJson(id = 1)}]"

        repository.refreshCharacters(listOf(1))

        assertEquals(1, repository.observeCharactersByIds(listOf(1)).first().size)
    }
}
