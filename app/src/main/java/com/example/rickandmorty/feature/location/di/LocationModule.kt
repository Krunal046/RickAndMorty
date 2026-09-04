package com.example.rickandmorty.feature.location.di

import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.location.data.local.dao.LocationDao
import com.example.rickandmorty.feature.location.data.remote.LocationApiService
import com.example.rickandmorty.feature.location.data.repository.LocationRepositoryImpl
import com.example.rickandmorty.feature.location.domain.repository.LocationRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Everything the location feature contributes to the graph. */
@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: LocationRepositoryImpl): LocationRepository

    companion object {

        @Provides
        @Singleton
        fun provideLocationApiService(retrofit: Retrofit): LocationApiService =
            retrofit.create(LocationApiService::class.java)

        @Provides
        fun provideLocationDao(database: RickAndMortyDatabase): LocationDao =
            database.locationDao()
    }
}
