package com.example.rickandmorty.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface RemoteKeyDao {

    @Upsert
    suspend fun upsert(remoteKey: RemoteKeyEntity)

    @Query("SELECT * FROM remote_keys WHERE queryKey = :queryKey")
    suspend fun remoteKey(queryKey: String): RemoteKeyEntity?

    @Query("DELETE FROM remote_keys WHERE queryKey = :queryKey")
    suspend fun clear(queryKey: String)

    /**
     * Filtered lists for one resource, oldest first, beyond the [keep] most recently used.
     *
     * Every distinct search is cached under its own key, so without a bound the database
     * would grow for the lifetime of the install. The unfiltered list is excluded by the
     * `:resource || ':%'` pattern - it is the app's default screen and is never evicted.
     */
    @Query(
        """
        SELECT queryKey FROM remote_keys
        WHERE queryKey LIKE :resource || ':%'
        ORDER BY lastUpdated DESC
        LIMIT -1 OFFSET :keep
        """
    )
    suspend fun staleFilteredKeys(resource: String, keep: Int): List<String>

    @Query("DELETE FROM remote_keys WHERE queryKey IN (:queryKeys)")
    suspend fun clearAll(queryKeys: List<String>)
}
