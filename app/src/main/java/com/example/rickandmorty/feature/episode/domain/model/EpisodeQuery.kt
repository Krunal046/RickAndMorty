package com.example.rickandmorty.feature.episode.domain.model

/**
 * What the user is currently asking the episode list for: spec S4's single search box,
 * which the API serves through two different parameters.
 *
 * `?name=` matches a title and `?episode=` matches the `S01E01` code, and the API will not
 * accept one in place of the other. Rather than make the user pick, the text is routed by
 * what it looks like: anything starting with an S followed by a digit is a code, everything
 * else is a title. That covers `S01E01` and the looser `S02` the API also matches, and no
 * real episode title begins that way.
 *
 * [cacheKey] is the identity of the resulting list in the database, and follows the same
 * rule as `CharacterQuery`: two searches that mean the same thing must produce the same key
 * or they would cache twice, so the text is trimmed and lowercased - the API matches
 * case-insensitively - and the parameter name is part of the key, because `name=s01` and
 * `episode=s01` are different questions.
 */
data class EpisodeQuery(val search: String = "") {

    val isEmpty: Boolean
        get() = search.isBlank()

    /** True when the search text reads as an episode code rather than a title. */
    val isCodeSearch: Boolean
        get() = CODE_SHAPE.containsMatchIn(search.trim())

    /** The `name` parameter, or null when the text is a code or absent. */
    fun nameOrNull(): String? = trimmedOrNull()?.takeUnless { isCodeSearch }

    /** The `episode` parameter, or null when the text is a title or absent. */
    fun codeOrNull(): String? = trimmedOrNull()?.takeIf { isCodeSearch }

    val cacheKey: String
        get() {
            val text = trimmedOrNull() ?: return RESOURCE
            val parameter = if (isCodeSearch) "episode" else "name"

            return "$RESOURCE:$parameter=${text.lowercase()}"
        }

    private fun trimmedOrNull(): String? = search.trim().takeIf { it.isNotBlank() }

    companion object {
        const val RESOURCE = "episode"

        /**
         * The cache an episode opened on its own is kept under (spec E2), as opposed to one
         * that arrived as part of a list.
         *
         * Like `CharacterQuery.DETAIL` it is never written to `remote_keys` - it is not a
         * paged list and has no cursor - so `RemoteKeyDao.staleFilteredKeys` can never
         * return it and the eviction that trims old searches cannot reach it. That is what
         * lets an episode reached from a character's chips still open after the search that
         * cached it has aged out.
         */
        const val DETAIL = "$RESOURCE:detail"

        /**
         * The cache the batch endpoint writes to, holding episodes pulled in by id for a
         * character's episode list (spec C4).
         *
         * Out of reach of the eviction for the same reason as [DETAIL].
         */
        const val BY_ID = "$RESOURCE:byId"

        /** Starts with an S followed by a digit - `S01E01`, `S02`, `s3e7`. */
        private val CODE_SHAPE = Regex("^[sS]\\d")
    }
}
