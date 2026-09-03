package com.example.rickandmorty.feature.character.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.mapper.toDomain
import com.example.rickandmorty.feature.character.data.paging.CharacterRemoteMediator
import com.example.rickandmorty.feature.character.data.remote.CharacterApiService
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val characterApi: CharacterApiService,
    private val characterDao: CharacterDao,
    private val database: RickAndMortyDatabase
) : CharacterRepository {

    override fun characterPaging(): Flow<PagingData<CharacterModel>> {
        val pageQuery = PLAIN_LIST_QUERY

        return Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                // Paging defaults initialLoadSize to 3x pageSize, but the API's page size is
                // fixed and it ignores loadSize - asking for more only triggers extra loads.
                initialLoadSize = PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                enablePlaceholders = false
            ),
            remoteMediator = CharacterRemoteMediator(
                pageQuery = pageQuery,
                database = database,
                characterDao = characterDao,
                fetchPage = { page -> characterApi.getCharacterList(page) }
            ),
            // A fresh source per call: a PagingSource is single-use and Paging invalidates
            // and re-creates it whenever the table changes.
            pagingSourceFactory = { characterDao.pagingSource(pageQuery) }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override suspend fun getCharacterById(id: Int): Resource<CharacterModel> =
        safeApiCall { characterApi.getCharacterById(id).toDomain() }

    override suspend fun getSelectedCharacterList(ids: String): Resource<List<CharacterModel>> =
        safeApiCall { characterApi.getSelectedCharacterList(ids).map { it.toDomain() } }

    private companion object {
        /** The page size the Rick and Morty API serves; it is not configurable. */
        const val PAGE_SIZE = 20
        const val PREFETCH_DISTANCE = 5

        /** Cache key for the unfiltered list. Phase 3 adds the filtered variants. */
        const val PLAIN_LIST_QUERY = "character"
    }
}
