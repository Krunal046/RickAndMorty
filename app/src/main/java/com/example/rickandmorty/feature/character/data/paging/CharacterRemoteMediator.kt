package com.example.rickandmorty.feature.character.data.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.rickandmorty.core.common.DataErrorException
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.database.RemoteKeyEntity
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.local.entity.CharacterEntity
import com.example.rickandmorty.feature.character.data.mapper.toEntity
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO

/**
 * Keeps the `characters` table stocked for one query; it never hands data to the UI.
 * Paging reads rows from the DAO, and this only decides when to go to the network and what
 * to write back - which is what makes the database the single source of truth.
 *
 * Everything is scoped by [pageQuery], so the plain list and each filtered search keep
 * their own rows and their own cursor and cannot overwrite one another.
 *
 * [fetchPage] is the raw, throwing API call. Wrapping it here rather than at the call site
 * keeps every failure on the same [safeApiCall] path as the rest of the app.
 */
@OptIn(ExperimentalPagingApi::class)
class CharacterRemoteMediator(
    private val pageQuery: String,
    private val database: RickAndMortyDatabase,
    private val characterDao: CharacterDao,
    private val fetchPage: suspend (page: Int) -> CharacterInfoDTO
) : RemoteMediator<Int, CharacterEntity>() {

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, CharacterEntity>
    ): MediatorResult {
        val page = when (loadType) {
            LoadType.REFRESH -> FIRST_PAGE

            // The API only pages forward and the cache always starts at page 1, so there is
            // never anything above the first row to load.
            LoadType.PREPEND -> return MediatorResult.Success(endOfPaginationReached = true)

            // A stored null means the API already reported info.next == null.
            LoadType.APPEND -> database.remoteKeyDao().remoteKey(pageQuery)?.nextPage
                ?: return MediatorResult.Success(endOfPaginationReached = true)
        }

        return when (val result = safeApiCall { fetchPage(page) }) {
            is Resource.Error -> MediatorResult.Error(DataErrorException(result.error))

            is Resource.Success -> {
                // info.next, not a page count, is what the API says ends the list.
                val nextPage = if (result.data.info.next == null) null else page + 1
                writePage(loadType, result.data, nextPage)
                MediatorResult.Success(endOfPaginationReached = nextPage == null)
            }
        }
    }

    /**
     * One transaction, so the rows and the cursor can never disagree. If this is interrupted
     * the next load sees the previous consistent state rather than a half-applied page.
     */
    private suspend fun writePage(
        loadType: LoadType,
        response: CharacterInfoDTO,
        nextPage: Int?
    ) {
        database.withTransaction {
            if (loadType == LoadType.REFRESH) {
                characterDao.clearForQuery(pageQuery)
                database.remoteKeyDao().clear(pageQuery)
            }

            // Continue the existing ordering on append; a refresh has just emptied the table.
            val startOrder = (characterDao.maxOrder(pageQuery) ?: -1) + 1

            characterDao.upsertAll(
                response.results.mapIndexed { index, dto ->
                    dto.toEntity(pageQuery = pageQuery, orderInQuery = startOrder + index)
                }
            )

            database.remoteKeyDao().upsert(
                RemoteKeyEntity(
                    queryKey = pageQuery,
                    nextPage = nextPage,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
