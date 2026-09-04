package com.example.rickandmorty.feature.character.data.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import androidx.room.withTransaction
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.common.toIdPath
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.core.network.decodeBatch
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.mapper.toDomain
import com.example.rickandmorty.feature.character.data.mapper.toEntity
import com.example.rickandmorty.feature.character.data.paging.CharacterRemoteMediator
import com.example.rickandmorty.feature.character.data.remote.CharacterApiService
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalPagingApi::class)
@Singleton
class CharacterRepositoryImpl @Inject constructor(
    private val characterApi: CharacterApiService,
    private val characterDao: CharacterDao,
    private val database: RickAndMortyDatabase,
    private val json: Json
) : CharacterRepository {

    override fun characterPaging(query: CharacterQuery): Flow<PagingData<CharacterModel>> {
        val pageQuery = query.cacheKey

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
                fetchPage = { page ->
                    characterApi.getCharacterList(
                        page = page,
                        name = query.nameOrNull(),
                        status = query.status?.apiValue,
                        species = query.speciesOrNull(),
                        gender = query.gender?.apiValue
                    )
                }
            ),
            // A fresh source per call: a PagingSource is single-use and Paging invalidates
            // and re-creates it whenever the table changes.
            pagingSourceFactory = { characterDao.pagingSource(pageQuery) }
        ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
    }

    override fun observeCharacter(id: Int): Flow<CharacterModel?> =
        characterDao.observeById(id).map { it?.toDomain() }

    override suspend fun refreshCharacter(id: Int): Resource<Unit> = safeApiCall {
        val character = characterApi.getCharacterById(id)

        database.withTransaction { cacheDetail(character) }
    }

    override fun observeCharactersByIds(ids: List<Int>): Flow<List<CharacterModel>> =
        if (ids.isEmpty()) {
            flowOf(emptyList())
        } else {
            characterDao.observeByIds(ids).map { rows -> rows.map { it.toDomain() } }
        }

    /**
     * The empty case has to short-circuit rather than build a path: `character/` is the
     * *paged list* endpoint, so asking for no characters would quietly download page one.
     */
    override suspend fun refreshCharacters(ids: List<Int>): Resource<Unit> {
        if (ids.isEmpty()) return Resource.Success(Unit)

        return safeApiCall {
            val characters =
                json.decodeBatch<CharacterDTO>(characterApi.getCharactersByIds(ids.toIdPath()))

            characterDao.upsertAll(
                characters.map { dto ->
                    dto.toEntity(
                        pageQuery = CharacterQuery.BY_ID,
                        // No list, so no list position; the row stores its own id so that
                        // re-fetching the same batch writes something byte-identical.
                        orderInQuery = dto.id
                    )
                }
            )
        }
    }

    /**
     * Writes a freshly fetched character over every cached copy of it at once.
     *
     * A character legitimately has one row per list that loaded it, and each row carries
     * that list's position. Upserting a single row would either reorder a list - the detail
     * knows no position and would write 0 - or leave the copy that `observeById` happens to
     * return stale. So each existing row is rewritten in place, keeping its own `pageQuery`
     * and `orderInQuery`, and the detail row is added on top for the case where no list has
     * cached this character at all.
     */
    private suspend fun cacheDetail(character: CharacterDTO) {
        val detail = character.toEntity(pageQuery = CharacterQuery.DETAIL, orderInQuery = 0)

        val rows = characterDao.rowsForId(character.id)
            .map { cached -> detail.copy(pageQuery = cached.pageQuery, orderInQuery = cached.orderInQuery) }
            .plus(detail)
            // The detail row is itself one of the cached copies after the first refresh.
            .distinctBy { it.pageQuery }

        characterDao.upsertAll(rows)
    }

    private companion object {
        /** The page size the Rick and Morty API serves; it is not configurable. */
        const val PAGE_SIZE = 20
        const val PREFETCH_DISTANCE = 5
    }
}
