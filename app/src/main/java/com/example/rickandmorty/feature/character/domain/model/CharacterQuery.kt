package com.example.rickandmorty.feature.character.domain.model

/**
 * What the user is currently asking the character list for: spec S2's search box plus its
 * status, species and gender filters, which the API lets you combine freely.
 *
 * [cacheKey] is the identity of the resulting list in the database. Two searches that mean
 * the same thing must produce the same key or they would cache twice, so the text is
 * trimmed and lowercased - the API matches names case-insensitively - and the parameters
 * are always written in the same order.
 */
data class CharacterQuery(
    val name: String = "",
    val status: CharacterStatus? = null,
    val species: String = "",
    val gender: Gender? = null
) {

    val isEmpty: Boolean
        get() = name.isBlank() && status == null && species.isBlank() && gender == null

    /** Count shown on the filter button; the search text is not a "filter". */
    val activeFilterCount: Int
        get() = listOfNotNull(status, gender).size + if (species.isNotBlank()) 1 else 0

    val cacheKey: String
        get() {
            if (isEmpty) return RESOURCE

            val parts = buildList {
                if (name.isNotBlank()) add("name=${name.trim().lowercase()}")
                status?.let { add("status=${it.apiValue}") }
                if (species.isNotBlank()) add("species=${species.trim().lowercase()}")
                gender?.let { add("gender=${it.apiValue}") }
            }

            return "$RESOURCE:${parts.joinToString("&")}"
        }

    /** Blank strings are dropped rather than sent as empty query parameters. */
    fun nameOrNull(): String? = name.trim().takeIf { it.isNotBlank() }

    fun speciesOrNull(): String? = species.trim().takeIf { it.isNotBlank() }

    companion object {
        const val RESOURCE = "character"

        /**
         * The cache a character opened on its own is kept under (spec C3), as opposed to
         * one that arrived as part of a list.
         *
         * It exists so a character reached from a filtered search still opens offline after
         * that search has been evicted, and so a detail opened from a deep link is cached at
         * all. Being absent from `remote_keys` - it is not a paged list and has no cursor -
         * it can never be returned by `RemoteKeyDao.staleFilteredKeys`, so the eviction that
         * trims old searches cannot reach it.
         */
        const val DETAIL = "$RESOURCE:detail"

        /**
         * The copy of a character pinned when it is favorited (spec X3).
         *
         * Every other row in the table is cache: a list refresh clears its own query's rows
         * before writing the new page, and the eviction that trims old searches drops theirs
         * outright. Either would take a favorited character's only row with it, and the
         * favorite would vanish from the tab it was saved to. Like [DETAIL] this key is
         * never written to `remote_keys`, so neither can reach it.
         */
        const val FAVORITE = "$RESOURCE:favorite"

        /**
         * The cache the batch endpoint writes to, holding characters pulled in by id for an
         * episode's cast (spec E4) and a location's residents (L4).
         *
         * A grid of a relation has no list identity of its own to cache under, but it still
         * has to render offline - the app reads the database and only the database - so the
         * characters land in one shared bucket keyed by id, exactly as episodes do under
         * `EpisodeQuery.BY_ID`. Like [DETAIL] and [FAVORITE] it is never written to
         * `remote_keys`, so the eviction that trims old searches cannot reach it.
         */
        const val BY_ID = "$RESOURCE:byId"
    }
}
