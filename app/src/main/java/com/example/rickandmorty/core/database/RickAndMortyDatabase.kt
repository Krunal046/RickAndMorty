package com.example.rickandmorty.core.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity

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
        CharacterEntity::class
    ],
    version = 2,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2)
    ]
)
@TypeConverters(Converters::class)
abstract class RickAndMortyDatabase : RoomDatabase() {

    abstract fun remoteKeyDao(): RemoteKeyDao

    abstract fun characterDao(): CharacterDao

    companion object {
        const val NAME = "rick_and_morty.db"
    }
}
