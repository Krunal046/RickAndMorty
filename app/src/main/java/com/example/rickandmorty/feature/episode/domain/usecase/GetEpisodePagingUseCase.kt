package com.example.rickandmorty.feature.episode.domain.usecase

import androidx.paging.PagingData
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Spec E1 and E3: the paged episode list, plain or searched. */
class GetEpisodePagingUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {

    operator fun invoke(query: EpisodeQuery = EpisodeQuery()): Flow<PagingData<EpisodeModel>> =
        episodeRepository.episodePaging(query)
}
