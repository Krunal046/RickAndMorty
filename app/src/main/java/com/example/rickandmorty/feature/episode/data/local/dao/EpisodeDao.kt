package com.example.rickandmorty.feature.episode.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    /**
     * The episode list's actual source of data. Room regenerates this and invalidates Paging
     * on every write, so a refresh lands on screen without the UI asking for it.
     */
    @Query("SELECT * FROM episodes WHERE pageQuery = :pageQuery ORDER BY orderInQuery ASC")
    fun pagingSource(pageQuery: String): PagingSource<Int, EpisodeEntity>

    @Upsert
    suspend fun upsertAll(episodes: List<EpisodeEntity>)

    @Query("DELETE FROM episodes WHERE pageQuery = :pageQuery")
    suspend fun clearForQuery(pageQuery: String)

    @Query("DELETE FROM episodes WHERE pageQuery IN (:pageQueries)")
    suspend fun clearForQueries(pageQueries: List<String>)

    /** Highest position stored so far, so an appended page continues the ordering. */
    @Query("SELECT MAX(orderInQuery) FROM episodes WHERE pageQuery = :pageQuery")
    suspend fun maxOrder(pageQuery: String): Int?

    /**
     * The episodes behind a character's episode chips, read from the cache and only from it.
     *
     * `GROUP BY id` collapses the copies an episode may hold under several page queries -
     * they carry the same episode, so any one of them will do, exactly as
     * `CharacterDao.observeById` takes any cached copy of a character.
     *
     * Ordered by id because the API numbers episodes in airing order, which is the order the
     * chips should read in; the ids arrive that way from the character too.
     */
    @Query("SELECT * FROM episodes WHERE id IN (:ids) GROUP BY id ORDER BY id ASC")
    fun observeByIds(ids: List<Int>): Flow<List<EpisodeEntity>>

    /** Any cached copy will do for the detail screen, whichever list happened to load it. */
    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1")
    fun observeById(id: Int): Flow<EpisodeEntity?>

    /**
     * Every cached copy of one episode - one row per list that loaded it, plus the detail
     * row. A detail refresh rewrites all of them together so that the copy [observeById]
     * happens to pick cannot be the stale one.
     */
    @Query("SELECT * FROM episodes WHERE id = :id")
    suspend fun rowsForId(id: Int): List<EpisodeEntity>

    @Query("SELECT COUNT(*) FROM episodes WHERE pageQuery = :pageQuery")
    suspend fun countForQuery(pageQuery: String): Int
}
