package com.example.rickandmorty.feature.character.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    /**
     * The screen's actual source of data. Room regenerates this and invalidates Paging on
     * every write, so a refresh lands on screen without the UI asking for it.
     */
    @Query("SELECT * FROM characters WHERE pageQuery = :pageQuery ORDER BY orderInQuery ASC")
    fun pagingSource(pageQuery: String): PagingSource<Int, CharacterEntity>

    @Upsert
    suspend fun upsertAll(characters: List<CharacterEntity>)

    @Query("DELETE FROM characters WHERE pageQuery = :pageQuery")
    suspend fun clearForQuery(pageQuery: String)

    /** Highest position stored so far, so an appended page continues the ordering. */
    @Query("SELECT MAX(orderInQuery) FROM characters WHERE pageQuery = :pageQuery")
    suspend fun maxOrder(pageQuery: String): Int?

    @Query("SELECT COUNT(*) FROM characters WHERE pageQuery = :pageQuery")
    suspend fun countForQuery(pageQuery: String): Int

    /**
     * Any cached copy of the character will do for the detail screen, whichever list
     * happened to load it.
     */
    @Query("SELECT * FROM characters WHERE id = :id LIMIT 1")
    fun observeById(id: Int): Flow<CharacterEntity?>
}
