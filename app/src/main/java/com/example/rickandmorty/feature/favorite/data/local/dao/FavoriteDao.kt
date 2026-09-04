package com.example.rickandmorty.feature.favorite.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import com.example.rickandmorty.feature.favorite.data.local.entity.FavoriteCharacterEntity
import kotlinx.coroutines.flow.Flow

/**
 * Reads across both tables, so it returns a `CharacterEntity` rather than a favorite: what
 * the tab renders is characters, and the favorites table only says which ones.
 */
@Dao
interface FavoriteDao {

    @Upsert
    suspend fun upsert(favorite: FavoriteCharacterEntity)

    @Query("DELETE FROM character_favorites WHERE characterId = :characterId")
    suspend fun delete(characterId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM character_favorites WHERE characterId = :characterId)")
    fun observeIsFavorite(characterId: Int): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM character_favorites WHERE characterId = :characterId)")
    suspend fun isFavorite(characterId: Int): Boolean

    /**
     * Spec S8: saved characters, newest first, joined against whatever the cache holds.
     *
     * `GROUP BY c.id` collapses the copies a character keeps - one per list that loaded it,
     * plus the detail and pinned copies - so a favorite appears once however many lists it
     * has been seen in. The join is an inner one, so a favorite with nothing cached behind
     * it simply does not render; the repository pins a copy precisely so that cannot happen.
     */
    @Query(
        """
        SELECT c.* FROM characters c
        INNER JOIN character_favorites f ON f.characterId = c.id
        GROUP BY c.id
        ORDER BY f.favoritedAt DESC
        """
    )
    fun observeFavoriteCharacters(): Flow<List<CharacterEntity>>

    @Query("SELECT COUNT(*) FROM character_favorites")
    suspend fun count(): Int
}
