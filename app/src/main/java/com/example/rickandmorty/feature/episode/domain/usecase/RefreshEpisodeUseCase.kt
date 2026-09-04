package com.example.rickandmorty.feature.episode.domain.usecase

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import javax.inject.Inject

/** Fetches one episode and writes it to the cache. */
class RefreshEpisodeUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {

    suspend operator fun invoke(id: Int): Resource<Unit> = episodeRepository.refreshEpisode(id)
}
