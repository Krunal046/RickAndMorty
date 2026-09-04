package com.example.rickandmorty.feature.episode.domain.usecase

import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * One episode as the cache holds it (spec E2).
 *
 * Reading and refreshing are separate use cases because they are separate concerns: this is
 * what the screen renders, [RefreshEpisodeUseCase] is what keeps it current.
 */
class ObserveEpisodeUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {

    operator fun invoke(id: Int): Flow<EpisodeModel?> = episodeRepository.observeEpisode(id)
}
