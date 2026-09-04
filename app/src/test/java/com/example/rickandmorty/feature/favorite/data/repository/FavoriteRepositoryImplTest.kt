package com.example.rickandmorty.feature.favorite.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.database.RemoteKeyEntity
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.character.data.characterDto
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.mapper.toEntity
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.favorite.data.local.dao.FavoriteDao
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

/**
 * Spec X3's one hard rule: "a refresh must not evict a favorited character row, or the
 * favorite vanishes from this tab". Most of what follows is that rule, from each of the
 * directions it can be broken.
 */
@RunWith(RobolectricTestRunner::class)
class FavoriteRepositoryImplTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var characterDao: CharacterDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var repository: FavoriteRepositoryImpl

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        characterDao = database.characterDao()
        favoriteDao = database.favoriteDao()
        repository = FavoriteRepositoryImpl(
            favoriteDao = favoriteDao,
            characterDao = characterDao,
            database = database
        )
    }

    @After
    fun tearDown() = database.close()

    private suspend fun cache(id: Int, query: String, name: String = "Character $id") {
        characterDao.upsertAll(
            listOf(characterDto(id = id, name = name).toEntity(query, orderInQuery = id))
        )
    }

    @Test
    fun `favoriting a character puts it in the tab`() = runTest {
        cache(id = 1, query = "character")

        repository.toggleFavorite(1)

        assertEquals(
            listOf("Character 1"),
            repository.observeFavorites().first().map(CharacterModel::name)
        )
    }

    @Test
    fun `toggling the same character twice removes it again`() = runTest {
        cache(id = 1, query = "character")

        repository.toggleFavorite(1)
        repository.toggleFavorite(1)

        assertTrue(repository.observeFavorites().first().isEmpty())
        assertFalse(repository.observeIsFavorite(1).first())
    }

    /**
     * The rule itself. A character favorited from a search survives that list being
     * refreshed out from under it - which is what a `RemoteMediator` does on every open.
     */
    @Test
    fun `a favorite survives a refresh of the only list that cached it`() = runTest {
        cache(id = 1, query = "character:name=rick")
        repository.toggleFavorite(1)

        // What CharacterRemoteMediator does before writing a new page.
        characterDao.clearForQuery("character:name=rick")

        assertEquals(1, repository.observeFavorites().first().size)
    }

    /** The other way the row can go: eviction of an aged-out search. */
    @Test
    fun `a favorite survives the eviction that trims old searches`() = runTest {
        cache(id = 1, query = "character:name=rick")
        repository.toggleFavorite(1)
        database.remoteKeyDao().upsert(
            RemoteKeyEntity(queryKey = "character:name=rick", nextPage = null, lastUpdated = 1L)
        )

        val stale = database.remoteKeyDao()
            .staleFilteredKeys(CharacterQuery.RESOURCE, keep = 0)
        characterDao.clearForQueries(stale)

        assertEquals(listOf("character:name=rick"), stale)
        assertEquals(1, repository.observeFavorites().first().size)
    }

    @Test
    fun `un-favoriting releases the pinned copy rather than leaving it behind`() = runTest {
        cache(id = 1, query = "character")
        repository.toggleFavorite(1)
        assertEquals(1, characterDao.countForQuery(CharacterQuery.FAVORITE))

        repository.toggleFavorite(1)

        assertEquals(0, characterDao.countForQuery(CharacterQuery.FAVORITE))
    }

    /** Pinning must not disturb the lists the character is also in. */
    @Test
    fun `pinning leaves the character's place in its lists alone`() = runTest {
        cache(id = 7, query = "character")
        repository.toggleFavorite(7)

        val listRow = characterDao.rowsForId(7).single { it.pageQuery == "character" }
        assertEquals(7, listRow.orderInQuery)
    }

    /** A character cached by two lists is one favorite, not two rows in the tab. */
    @Test
    fun `a character cached by several lists appears in the tab once`() = runTest {
        cache(id = 1, query = "character")
        cache(id = 1, query = "character:name=rick")

        repository.toggleFavorite(1)

        assertEquals(1, repository.observeFavorites().first().size)
    }

    @Test
    fun `the pinned copy is taken from the detail row when there is one`() = runTest {
        cache(id = 1, query = "character", name = "Stale list copy")
        cache(id = 1, query = CharacterQuery.DETAIL, name = "Fresh detail copy")

        repository.toggleFavorite(1)

        val pinned = characterDao.rowsForId(1).single { it.pageQuery == CharacterQuery.FAVORITE }
        assertEquals("Fresh detail copy", pinned.name)
    }

    @Test
    fun `several favorites all reach the tab`() = runTest {
        cache(id = 1, query = "character", name = "First")
        cache(id = 2, query = "character", name = "Second")

        repository.toggleFavorite(1)
        repository.toggleFavorite(2)

        assertEquals(
            setOf("First", "Second"),
            repository.observeFavorites().first().map(CharacterModel::name).toSet()
        )
    }

    @Test
    fun `observeIsFavorite reports the saved state`() = runTest {
        cache(id = 1, query = "character")

        assertFalse(repository.observeIsFavorite(1).first())

        repository.toggleFavorite(1)

        assertTrue(repository.observeIsFavorite(1).first())
    }

    /** Nothing cached means nothing to pin; the toggle must not blow up on it. */
    @Test
    fun `favoriting a character with nothing cached does not fail`() = runTest {
        repository.toggleFavorite(404)

        assertTrue(repository.observeIsFavorite(404).first())
        assertTrue(repository.observeFavorites().first().isEmpty())
        assertEquals(0, characterDao.rowsForId(404).size)
    }
}
