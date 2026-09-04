package com.example.rickandmorty.feature.character.data.local

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.database.RemoteKeyEntity
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.character.data.characterDto
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import com.example.rickandmorty.feature.character.data.mapper.toEntity
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CharacterDaoTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: CharacterDao

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

    private fun entities(query: String, ids: List<Int>, startOrder: Int = 0) =
        ids.mapIndexed { index, id ->
            characterDto(id = id, name = "Character $id")
                .toEntity(pageQuery = query, orderInQuery = startOrder + index)
        }

    @Test
    fun `pages rows back in the order the api sent them, not by id`() = runTest {
        // Ids deliberately out of ascending order: the API's ordering is what the user sees.
        dao.upsertAll(entities("character", listOf(30, 10, 20)))

        val page = dao.pagingSource("character").load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(listOf(30, 10, 20), page.data.map(CharacterEntity::id))
    }

    /**
     * The reason `pageQuery` is part of the primary key: the same character legitimately
     * belongs to several lists, and refreshing one must leave the others intact.
     */
    @Test
    fun `the same character can belong to two queries at once`() = runTest {
        dao.upsertAll(entities("character", listOf(1, 2)))
        dao.upsertAll(entities("character:name=rick", listOf(1)))

        assertEquals(2, dao.countForQuery("character"))
        assertEquals(1, dao.countForQuery("character:name=rick"))
    }

    @Test
    fun `clearing one query leaves the others untouched`() = runTest {
        dao.upsertAll(entities("character", listOf(1, 2)))
        dao.upsertAll(entities("character:name=rick", listOf(1)))

        dao.clearForQuery("character:name=rick")

        assertEquals(2, dao.countForQuery("character"))
        assertEquals(0, dao.countForQuery("character:name=rick"))
    }

    @Test
    fun `maxOrder lets an appended page continue the ordering`() = runTest {
        dao.upsertAll(entities("character", listOf(1, 2, 3)))

        assertEquals(2, dao.maxOrder("character"))
    }

    @Test
    fun `maxOrder is null for a query with no rows yet`() = runTest {
        assertNull(dao.maxOrder("character"))
    }

    @Test
    fun `upsert replaces a row rather than duplicating it`() = runTest {
        dao.upsertAll(entities("character", listOf(1)))
        dao.upsertAll(
            listOf(
                characterDto(id = 1, name = "Renamed", status = "Dead")
                    .toEntity(pageQuery = "character", orderInQuery = 0)
            )
        )

        assertEquals(1, dao.countForQuery("character"))
        assertEquals("Renamed", dao.observeById(1).first()?.name)
    }

    /** The detail screen takes whichever cached copy exists, from any list. */
    @Test
    fun `observeById finds a character cached by a filtered list`() = runTest {
        dao.upsertAll(entities("character:name=rick", listOf(42)))

        assertEquals("Character 42", dao.observeById(42).first()?.name)
    }

    @Test
    fun `observeById emits null for a character that was never cached`() = runTest {
        assertNull(dao.observeById(999).first())
    }

    /** What a detail refresh reads before rewriting every copy in place. */
    @Test
    fun `rowsForId returns one row per list that cached the character`() = runTest {
        dao.upsertAll(entities("character", listOf(1)))
        dao.upsertAll(entities("character:name=rick", listOf(1)))
        dao.upsertAll(entities(CharacterQuery.DETAIL, listOf(1)))

        assertEquals(
            listOf("character", "character:name=rick", CharacterQuery.DETAIL).sorted(),
            dao.rowsForId(1).map(CharacterEntity::pageQuery).sorted()
        )
    }

    @Test
    fun `rowsForId is empty for a character that was never cached`() = runTest {
        assertTrue(dao.rowsForId(999).isEmpty())
    }

    /**
     * The detail cache has no cursor and is never written to `remote_keys`, so the eviction
     * that trims old searches cannot see it - which is what lets a character opened from a
     * search still open offline after that search has been evicted.
     */
    @Test
    fun `trimming stale searches cannot reach the detail cache`() = runTest {
        dao.upsertAll(entities("character:name=rick", listOf(1)))
        dao.upsertAll(entities(CharacterQuery.DETAIL, listOf(1)))
        database.remoteKeyDao().upsert(
            RemoteKeyEntity(queryKey = "character:name=rick", nextPage = null, lastUpdated = 1L)
        )

        val stale = database.remoteKeyDao().staleFilteredKeys(CharacterQuery.RESOURCE, keep = 0)
        dao.clearForQueries(stale)

        assertEquals(listOf("character:name=rick"), stale)
        assertEquals(1, dao.countForQuery(CharacterQuery.DETAIL))
    }
}
