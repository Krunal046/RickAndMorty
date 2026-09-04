package com.example.rickandmorty.feature.episode.domain.usecase

import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * The episodes a character appears in (spec C4), read from the cache.
 *
 * Reading and refreshing are separate use cases because they are separate concerns: this
 * one is what the screen renders, [RefreshEpisodesUseCase] is what keeps it current.
 */
class GetEpisodesByIdsUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {

    operator fun invoke(ids: List<Int>): Flow<List<EpisodeModel>> =
        episodeRepository.observeEpisodes(ids)
}
