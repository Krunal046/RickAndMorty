package com.example.rickandmorty.feature.character.di

import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.remote.CharacterApiService
import com.example.rickandmorty.feature.character.data.repository.CharacterRepositoryImpl
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/**
 * Everything the character feature contributes to the graph. Each feature owns its own
 * module, so adding Episodes or Locations does not mean editing a shared file that then
 * imports from every feature in the app.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CharacterModule {

    @Binds
    @Singleton
    abstract fun bindCharacterRepository(impl: CharacterRepositoryImpl): CharacterRepository

    companion object {

        @Provides
        @Singleton
        fun provideCharacterApiService(retrofit: Retrofit): CharacterApiService =
            retrofit.create(CharacterApiService::class.java)

        @Provides
        fun provideCharacterDao(database: RickAndMortyDatabase): CharacterDao =
            database.characterDao()
    }
}
