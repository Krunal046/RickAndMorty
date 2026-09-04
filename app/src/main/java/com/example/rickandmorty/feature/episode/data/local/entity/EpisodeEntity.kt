package com.example.rickandmorty.feature.episode.data.local.entity

import androidx.room.Entity

/**
 * An episode as the database stores it. Mirrors `CharacterEntity` deliberately: the two
 * resources page and cache the same way, and a reader who has understood one has understood
 * the other.
 *
 * [pageQuery] is part of the primary key for the same reason it is there - one table backs
 * every episode list, and refreshing one list must not disturb another's rows. It held that
 * shape from Phase 4, before there was any episode paging to need it, because Room cannot
 * auto-migrate a change to a primary key.
 *
 * The keys it takes - the plain list, a search, `EpisodeQuery.DETAIL`, `EpisodeQuery.BY_ID`
 * - are named by `EpisodeQuery`, which owns the identity of an episode list in the same way
 * `CharacterQuery` owns a character list's.
 */
@Entity(tableName = "episodes", primaryKeys = ["id", "pageQuery"])
data class EpisodeEntity(
    val id: Int,
    val pageQuery: String,
    val name: String,
    val airDate: String,
    /** The `S01E01` code. */
    val code: String,
    val characterIds: List<Int>,
    val url: String,
    val created: String,
    /**
     * Position within a paged list. A row under `EpisodeQuery.BY_ID` belongs to no list, so
     * it stores its own id here instead: re-fetching the same batch then produces a
     * byte-identical row, and Room does not invalidate its readers over a write that changed
     * nothing.
     */
    val orderInQuery: Int
)
