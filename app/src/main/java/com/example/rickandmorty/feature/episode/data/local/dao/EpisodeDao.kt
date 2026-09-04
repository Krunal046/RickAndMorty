package com.example.rickandmorty.feature.episode.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodeDao {

    @Upsert
    suspend fun upsertAll(episodes: List<EpisodeEntity>)

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

    @Query("SELECT COUNT(*) FROM episodes WHERE pageQuery = :pageQuery")
    suspend fun countForQuery(pageQuery: String): Int
}
