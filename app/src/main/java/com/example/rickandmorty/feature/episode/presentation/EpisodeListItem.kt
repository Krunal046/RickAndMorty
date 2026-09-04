package com.example.rickandmorty.feature.episode.presentation

import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel

/**
 * A row of the episode list: either an episode or the season heading that precedes one.
 *
 * Spec E1 groups by season, and the list is paged, so the headings cannot be computed by
 * grouping a list that is fully in memory - there is no such list. They are inserted into
 * the stream instead, by `PagingData.insertSeparators`, which is why a heading has to be a
 * kind of list item rather than something the screen wraps around one.
 */
sealed interface EpisodeListItem {

    /** [season] is null for an episode whose code the API did not write as `S01E01`. */
    data class Header(val season: Int?) : EpisodeListItem

    data class Item(val episode: EpisodeModel) : EpisodeListItem
}
