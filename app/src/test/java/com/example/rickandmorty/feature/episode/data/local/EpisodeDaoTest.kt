package com.example.rickandmorty.feature.episode.data.local

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.episode.data.episodeDto
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import com.example.rickandmorty.feature.episode.data.mapper.toEntity
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
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
class EpisodeDaoTest {

    private lateinit var database: RickAndMortyDatabase
    private lateinit var dao: EpisodeDao

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

    private fun rows(query: String, ids: List<Int>) = ids.map { id ->
        episodeDto(id = id, name = "Episode $id").toEntity(pageQuery = query, orderInQuery = id)
    }

    @Test
    fun `reads back only the episodes that were asked for`() = runTest {
        dao.upsertAll(rows(EpisodeQuery.BY_ID, listOf(1, 2, 3)))

        val episodes = dao.observeByIds(listOf(1, 3)).first()

        assertEquals(listOf(1, 3), episodes.map(EpisodeEntity::id))
    }

    /** Chips should read in airing order, which is id order for this API. */
    @Test
    fun `orders by id whatever order the ids were asked in`() = runTest {
        dao.upsertAll(rows(EpisodeQuery.BY_ID, listOf(1, 2, 3)))

        val episodes = dao.observeByIds(listOf(3, 1, 2)).first()

        assertEquals(listOf(1, 2, 3), episodes.map(EpisodeEntity::id))
    }

    /**
     * An episode is cached once per list that loaded it. The detail screen wants the episode,
     * not one row per list, so the copies collapse to one.
     */
    @Test
    fun `an episode cached under two queries is returned once`() = runTest {
        dao.upsertAll(rows(EpisodeQuery.BY_ID, listOf(1)))
        dao.upsertAll(rows("episode:name=pilot", listOf(1)))

        assertEquals(1, dao.observeByIds(listOf(1)).first().size)
    }

    @Test
    fun `emits nothing for episodes that were never cached`() = runTest {
        assertTrue(dao.observeByIds(listOf(99)).first().isEmpty())
    }

    @Test
    fun `re-fetching the same batch replaces the rows rather than duplicating them`() = runTest {
        dao.upsertAll(rows(EpisodeQuery.BY_ID, listOf(1, 2)))
        dao.upsertAll(rows(EpisodeQuery.BY_ID, listOf(1, 2)))

        assertEquals(2, dao.countForQuery(EpisodeQuery.BY_ID))
    }

    /** Whichever list happened to cache it will do for the detail screen. */
    @Test
    fun `the detail reads any cached copy of an episode`() = runTest {
        dao.upsertAll(rows("episode:name=pilot", listOf(7)))

        assertEquals(7, dao.observeById(7).first()?.id)
    }

    @Test
    fun `the detail emits null while nothing is cached`() = runTest {
        assertNull(dao.observeById(7).first())
    }

    /**
     * A detail refresh has to rewrite every copy together, so it first has to be able to
     * find them all - the copy `observeById` picks is otherwise free to be the stale one.
     */
    @Test
    fun `every cached copy of an episode is reachable for a rewrite`() = runTest {
        dao.upsertAll(rows(EpisodeQuery.RESOURCE, listOf(1)))
        dao.upsertAll(rows("episode:name=pilot", listOf(1)))
        dao.upsertAll(rows(EpisodeQuery.DETAIL, listOf(1)))

        assertEquals(
            listOf(EpisodeQuery.RESOURCE, EpisodeQuery.DETAIL, "episode:name=pilot"),
            dao.rowsForId(1).map { it.pageQuery }.sorted()
        )
    }

    /**
     * Paging reads rows in the order they arrived, not by id: sorting by id would reorder a
     * search, and SQLite gives no guarantee of insertion order without an explicit sort.
     */
    @Test
    fun `the paging source is scoped to one query and ordered by position`() = runTest {
        dao.upsertAll(rows(EpisodeQuery.RESOURCE, listOf(3, 1, 2)))
        dao.upsertAll(rows("episode:name=pilot", listOf(9)))

        val page = dao.pagingSource(EpisodeQuery.RESOURCE).load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 10, placeholdersEnabled = false)
        ) as PagingSource.LoadResult.Page

        assertEquals(listOf(1, 2, 3), page.data.map { it.id })
    }
}
