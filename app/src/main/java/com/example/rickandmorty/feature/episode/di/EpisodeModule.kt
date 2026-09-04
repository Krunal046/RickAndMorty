package com.example.rickandmorty.feature.episode.di

import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.episode.data.local.dao.EpisodeDao
import com.example.rickandmorty.feature.episode.data.remote.EpisodeApiService
import com.example.rickandmorty.feature.episode.data.repository.EpisodeRepositoryImpl
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Everything the episode feature contributes to the graph. */
@Module
@InstallIn(SingletonComponent::class)
abstract class EpisodeModule {

    @Binds
    @Singleton
    abstract fun bindEpisodeRepository(impl: EpisodeRepositoryImpl): EpisodeRepository

    companion object {

        @Provides
        @Singleton
        fun provideEpisodeApiService(retrofit: Retrofit): EpisodeApiService =
            retrofit.create(EpisodeApiService::class.java)

        @Provides
        fun provideEpisodeDao(database: RickAndMortyDatabase): EpisodeDao = database.episodeDao()
    }
}
