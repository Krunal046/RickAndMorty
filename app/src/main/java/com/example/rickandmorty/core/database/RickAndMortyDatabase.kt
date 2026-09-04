package com.example.rickandmorty.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.local.entity.EpisodeEntity
import com.example.rickandmorty.feature.favorite.data.local.dao.FavoriteDao
import com.example.rickandmorty.feature.favorite.data.local.entity.FavoriteCharacterEntity
import com.example.rickandmorty.feature.location.data.local.dao.LocationDao
import com.example.rickandmorty.feature.location.data.local.entity.LocationEntity

/**
 * The app's single source of truth. Screens read from here and only from here; the network
 * layer's job is to keep these tables current.
 *
 * Schema changes are handled by Room's auto-migrations rather than a destructive fallback:
 * most tables are a cache that could safely be dropped, but favorites are user data and
 * must survive an upgrade.
 */
@Database(
    entities = [
        RemoteKeyEntity::class,
        CharacterEntity::class,
        EpisodeEntity::class,
        FavoriteCharacterEntity::class,
        LocationEntity::class
    ],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        // v3 only adds the `episodes` table, which Room can generate on its own.
        AutoMigration(from = 2, to = 3),
        // v4 adds `character_favorites`. This one carries user data, so the auto-migration
        // is not a convenience - dropping and recreating would lose what the user saved.
        AutoMigration(from = 3, to = 4),
        // v5 only adds the `locations` table, which Room can generate on its own.
        AutoMigration(from = 4, to = 5)
    ]
)
@TypeConverters(Converters::class)
abstract class RickAndMortyDatabase : RoomDatabase() {

    abstract fun remoteKeyDao(): RemoteKeyDao

    abstract fun characterDao(): CharacterDao

    abstract fun episodeDao(): EpisodeDao

    abstract fun favoriteDao(): FavoriteDao

    abstract fun locationDao(): LocationDao

    companion object {
        const val NAME = "rick_and_morty.db"
    }
}
