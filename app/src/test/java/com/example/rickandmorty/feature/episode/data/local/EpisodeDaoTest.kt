package com.example.rickandmorty.feature.episode.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.episode.data.episodeDto
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import com.example.rickandmorty.feature.episode.data.mapper.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
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
        dao.upsertAll(rows(EpisodeEntity.BY_ID_QUERY, listOf(1, 2, 3)))

        val episodes = dao.observeByIds(listOf(1, 3)).first()

        assertEquals(listOf(1, 3), episodes.map(EpisodeEntity::id))
    }

    /** Chips should read in airing order, which is id order for this API. */
    @Test
    fun `orders by id whatever order the ids were asked in`() = runTest {
        dao.upsertAll(rows(EpisodeEntity.BY_ID_QUERY, listOf(1, 2, 3)))

        val episodes = dao.observeByIds(listOf(3, 1, 2)).first()

        assertEquals(listOf(1, 2, 3), episodes.map(EpisodeEntity::id))
    }

    /**
     * An episode is cached once per list that loaded it. The detail screen wants the episode,
     * not one row per list, so the copies collapse to one.
     */
    @Test
    fun `an episode cached under two queries is returned once`() = runTest {
        dao.upsertAll(rows(EpisodeEntity.BY_ID_QUERY, listOf(1)))
        dao.upsertAll(rows("episode:name=pilot", listOf(1)))

        assertEquals(1, dao.observeByIds(listOf(1)).first().size)
    }

    @Test
    fun `emits nothing for episodes that were never cached`() = runTest {
        assertTrue(dao.observeByIds(listOf(99)).first().isEmpty())
    }

    @Test
    fun `re-fetching the same batch replaces the rows rather than duplicating them`() = runTest {
        dao.upsertAll(rows(EpisodeEntity.BY_ID_QUERY, listOf(1, 2)))
        dao.upsertAll(rows(EpisodeEntity.BY_ID_QUERY, listOf(1, 2)))

        assertEquals(2, dao.countForQuery(EpisodeEntity.BY_ID_QUERY))
    }
}
