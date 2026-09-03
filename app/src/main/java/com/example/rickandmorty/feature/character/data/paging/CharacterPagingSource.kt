package com.example.rickandmorty.feature.character.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.rickandmorty.core.common.DataErrorException
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.core.network.safeApiCall
import com.example.rickandmorty.feature.character.data.remote.CharacterApiService
import com.example.rickandmorty.feature.character.data.mapper.toDomain
import com.example.rickandmorty.feature.character.domain.model.CharacterModel

/**
 * Pages the character list straight off the network. Keys are the API's own page numbers,
 * and the API's `info.next` — not a count or a page total — decides where the list ends.
 *
 * Errors go through [safeApiCall] like every other call, then travel to the UI as a
 * [DataErrorException] because Paging can only carry a [Throwable].
 */
class CharacterPagingSource(
    private val characterApi: CharacterApiService
) : PagingSource<Int, CharacterModel>() {

    override fun getRefreshKey(state: PagingState<Int, CharacterModel>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            val page = state.closestPageToPosition(anchorPosition)
            page?.prevKey?.plus(1) ?: page?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, CharacterModel> {
        val page = params.key ?: FIRST_PAGE

        return when (val result = safeApiCall { characterApi.getCharacterList(page) }) {
            is Resource.Success -> LoadResult.Page(
                data = result.data.results.map { it.toDomain() },
                prevKey = if (page == FIRST_PAGE) null else page - 1,
                nextKey = if (result.data.info.next == null) null else page + 1
            )

            is Resource.Error -> LoadResult.Error(DataErrorException(result.error))
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
