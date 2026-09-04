package com.example.rickandmorty.feature.location.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.rickandmorty.feature.location.data.local.entity.LocationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    /**
     * The location list's actual source of data. Room regenerates this and invalidates
     * Paging on every write, so a refresh lands on screen without the UI asking for it.
     */
    @Query("SELECT * FROM locations WHERE pageQuery = :pageQuery ORDER BY orderInQuery ASC")
    fun pagingSource(pageQuery: String): PagingSource<Int, LocationEntity>

    @Upsert
    suspend fun upsertAll(locations: List<LocationEntity>)

    @Query("DELETE FROM locations WHERE pageQuery = :pageQuery")
    suspend fun clearForQuery(pageQuery: String)

    @Query("DELETE FROM locations WHERE pageQuery IN (:pageQueries)")
    suspend fun clearForQueries(pageQueries: List<String>)

    /** Highest position stored so far, so an appended page continues the ordering. */
    @Query("SELECT MAX(orderInQuery) FROM locations WHERE pageQuery = :pageQuery")
    suspend fun maxOrder(pageQuery: String): Int?

    /** Any cached copy will do for the detail screen, whichever list happened to load it. */
    @Query("SELECT * FROM locations WHERE id = :id LIMIT 1")
    fun observeById(id: Int): Flow<LocationEntity?>

    /**
     * Every cached copy of one location - one row per list that loaded it, plus the detail
     * row. A detail refresh rewrites all of them together so that the copy [observeById]
     * happens to pick cannot be the stale one.
     */
    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun rowsForId(id: Int): List<LocationEntity>

    @Query("SELECT COUNT(*) FROM locations WHERE pageQuery = :pageQuery")
    suspend fun countForQuery(pageQuery: String): Int
}
