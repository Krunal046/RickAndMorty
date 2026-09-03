package com.example.rickandmorty.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
        RemoteKeyEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class RickAndMortyDatabase : RoomDatabase() {

    abstract fun remoteKeyDao(): RemoteKeyDao

    companion object {
        const val NAME = "rick_and_morty.db"
    }
}
