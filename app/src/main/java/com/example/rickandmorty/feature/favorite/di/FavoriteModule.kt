package com.example.rickandmorty.feature.favorite.di

import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.favorite.data.local.dao.FavoriteDao
import com.example.rickandmorty.feature.favorite.data.repository.FavoriteRepositoryImpl
import com.example.rickandmorty.feature.favorite.domain.repository.FavoriteRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Everything the favorites feature contributes. There is no API service to provide: spec X3
 * has no network path.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class FavoriteModule {

    @Binds
    @Singleton
    abstract fun bindFavoriteRepository(impl: FavoriteRepositoryImpl): FavoriteRepository

    companion object {

        @Provides
        fun provideFavoriteDao(database: RickAndMortyDatabase): FavoriteDao = database.favoriteDao()
    }
}
