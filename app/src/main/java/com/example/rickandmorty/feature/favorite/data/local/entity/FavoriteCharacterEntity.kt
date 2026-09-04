package com.example.rickandmorty.feature.favorite.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A character the user has saved (spec X3).
 *
 * Deliberately its own table rather than a column on `characters`: every row in that table
 * is cache that a refresh or an eviction may delete at any time, and a favorite is user
 * data. Keeping the two apart is what lets the cache be thrown away without losing what the
 * user chose to keep - and it is why the database uses auto-migrations rather than a
 * destructive fallback.
 *
 * It stores only the id. The character itself is read from the cache, which the repository
 * pins a copy into when a favorite is added.
 */
@Entity(tableName = "character_favorites")
data class FavoriteCharacterEntity(
    @PrimaryKey val characterId: Int,
    /** Most recently saved first is the order the tab reads in. */
    val favoritedAt: Long
)
