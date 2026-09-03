package com.example.rickandmorty.core.di

import android.content.Context
import androidx.room.Room
import com.example.rickandmorty.core.database.RemoteKeyDao
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * The database and the DAOs that are not owned by a single feature. A feature's own DAO is
 * provided by its module, from the database instance handed out here.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): RickAndMortyDatabase = Room.databaseBuilder(
        context = context,
        klass = RickAndMortyDatabase::class.java,
        name = RickAndMortyDatabase.NAME
    ).build()

    @Provides
    fun provideRemoteKeyDao(database: RickAndMortyDatabase): RemoteKeyDao = database.remoteKeyDao()
}
