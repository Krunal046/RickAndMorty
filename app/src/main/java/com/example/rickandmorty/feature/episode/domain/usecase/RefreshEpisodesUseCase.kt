package com.example.rickandmorty.feature.episode.domain.usecase

import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import javax.inject.Inject

/** Fills the episode cache for [ids] in one batch call. */
class RefreshEpisodesUseCase @Inject constructor(
    private val episodeRepository: EpisodeRepository
) {

    suspend operator fun invoke(ids: List<Int>): Resource<Unit> =
        episodeRepository.refreshEpisodes(ids)
}
