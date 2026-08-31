package com.example.rickandmorty.feature.character.domain.usecase

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCharacterPagingUseCase @Inject constructor(
    private val characterRepository: CharacterRepository
) {

    operator fun invoke(): Flow<PagingData<CharacterModel>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            // Paging defaults initialLoadSize to 3x pageSize, but the API's page size is
            // fixed and it ignores loadSize - asking for more only triggers extra loads.
            initialLoadSize = PAGE_SIZE,
            prefetchDistance = PREFETCH_DISTANCE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = characterRepository::characterPagingSource
    ).flow

    private companion object {
        /** The page size the Rick and Morty API serves; it is not configurable. */
        const val PAGE_SIZE = 20
        const val PREFETCH_DISTANCE = 5
    }
}
