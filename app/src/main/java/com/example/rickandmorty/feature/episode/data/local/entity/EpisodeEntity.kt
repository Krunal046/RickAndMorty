package com.example.rickandmorty.feature.episode.data.local.entity

import androidx.room.Entity

/**
 * An episode as the database stores it. Mirrors `CharacterEntity` deliberately: the two
 * resources page and cache the same way, and a reader who has understood one has understood
 * the other.
 *
 * [pageQuery] is part of the primary key for the same reason it is there - one table backs
 * every episode list, and refreshing one list must not disturb another's rows. It is in
 * place from this phase even though episode paging only arrives in Phase 6, because Room
 * cannot auto-migrate a change to a primary key: adding the column later would mean writing
 * a migration by hand for no gain.
 *
 * Episodes fetched by id rather than as part of a list are stored under [BY_ID_QUERY].
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
     * Position within a paged list. A row under [BY_ID_QUERY] belongs to no list, so it
     * stores its own id here instead: re-fetching the same batch then produces a byte-identical
     * row, and Room does not invalidate its readers over a write that changed nothing.
     */
    val orderInQuery: Int
) {
    companion object {
        const val RESOURCE = "episode"

        /**
         * The cache the batch endpoint writes to, holding episodes pulled in by id for a
         * character's episode list (spec C4) and later an episode's cast.
         *
         * It is never written to `remote_keys` - it is not a paged list and has no cursor -
         * so `RemoteKeyDao.staleFilteredKeys` can never return it and the eviction that
         * trims old searches cannot reach these rows.
         */
        const val BY_ID_QUERY = "$RESOURCE:byId"
    }
}
