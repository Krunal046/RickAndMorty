package com.example.rickandmorty.feature.favorite.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.character.data.characterDto
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import com.example.rickandmorty.feature.character.data.mapper.toEntity
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.favorite.data.local.dao.FavoriteDao
import com.example.rickandmorty.feature.favorite.data.local.entity.FavoriteCharacterEntity
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

@RunWith(RobolectricTestRunner::class)
class FavoriteDaoTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: FavoriteDao
    private lateinit var characterDao: CharacterDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RickAndMortyDatabase::class.java
        ).build()
        dao = database.favoriteDao()
        characterDao = database.characterDao()
    }

    @After
    fun tearDown() = database.close()

    private suspend fun pin(id: Int, name: String, favoritedAt: Long) {
        characterDao.upsertAll(
            listOf(
                characterDto(id = id, name = name)
                    .toEntity(CharacterQuery.FAVORITE, orderInQuery = 0)
            )
        )
        dao.upsert(FavoriteCharacterEntity(characterId = id, favoritedAt = favoritedAt))
    }

    /** Most recently saved first is the order spec S8 reads in. */
    @Test
    fun `returns favorites newest first`() = runTest {
        pin(id = 1, name = "Oldest", favoritedAt = 100L)
        pin(id = 2, name = "Newest", favoritedAt = 300L)
        pin(id = 3, name = "Middle", favoritedAt = 200L)

        assertEquals(
            listOf("Newest", "Middle", "Oldest"),
            dao.observeFavoriteCharacters().first().map(CharacterEntity::name)
        )
    }

    /**
     * A character is cached once per list that loaded it plus the pinned copy, and the tab
     * must show it once - not once per copy.
     */
    @Test
    fun `a character with several cached copies is returned once`() = runTest {
        pin(id = 1, name = "Rick", favoritedAt = 100L)
        characterDao.upsertAll(
            listOf(characterDto(id = 1, name = "Rick").toEntity("character", orderInQuery = 4))
        )
        characterDao.upsertAll(
            listOf(characterDto(id = 1, name = "Rick").toEntity(CharacterQuery.DETAIL, 0))
        )

        assertEquals(1, dao.observeFavoriteCharacters().first().size)
    }

    /** The join is inner, so a favorite with no cached character simply does not render. */
    @Test
    fun `a favorite with nothing cached behind it is skipped`() = runTest {
        dao.upsert(FavoriteCharacterEntity(characterId = 99, favoritedAt = 100L))

        assertTrue(dao.observeFavoriteCharacters().first().isEmpty())
        assertTrue(dao.isFavorite(99))
    }

    @Test
    fun `deleting a favorite takes it out of the tab`() = runTest {
        pin(id = 1, name = "Rick", favoritedAt = 100L)

        dao.delete(1)

        assertTrue(dao.observeFavoriteCharacters().first().isEmpty())
        assertFalse(dao.isFavorite(1))
    }

    @Test
    fun `upsert does not save the same character twice`() = runTest {
        pin(id = 1, name = "Rick", favoritedAt = 100L)
        dao.upsert(FavoriteCharacterEntity(characterId = 1, favoritedAt = 200L))

        assertEquals(1, dao.count())
    }

    @Test
    fun `observeIsFavorite is false for a character that was never saved`() = runTest {
        assertFalse(dao.observeIsFavorite(1).first())
    }
}
