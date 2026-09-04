package com.example.rickandmorty.feature.location.domain.model

/**
 * What the user is currently asking the location list for: spec S6's search box plus its
 * type and dimension filters, which the API lets you combine freely.
 *
 * [cacheKey] follows the same rule as `CharacterQuery` and `EpisodeQuery`: two searches that
 * mean the same thing must produce the same key or they would cache twice, so the text is
 * trimmed and lowercased - the API matches case-insensitively - and the parameters are
 * always written in the same order.
 */
data class LocationQuery(
    val name: String = "",
    val type: String = "",
    val dimension: String = ""
) {

    val isEmpty: Boolean
        get() = name.isBlank() && type.isBlank() && dimension.isBlank()

    /** Count shown on the filter button; the search text is not a "filter". */
    val activeFilterCount: Int
        get() = listOf(type, dimension).count { it.isNotBlank() }

    val cacheKey: String
        get() {
            if (isEmpty) return RESOURCE

            val parts = buildList {
                if (name.isNotBlank()) add("name=${name.trim().lowercase()}")
                if (type.isNotBlank()) add("type=${type.trim().lowercase()}")
                if (dimension.isNotBlank()) add("dimension=${dimension.trim().lowercase()}")
            }

            return "$RESOURCE:${parts.joinToString("&")}"
        }

    /** Blank strings are dropped rather than sent as empty query parameters. */
    fun nameOrNull(): String? = name.trim().takeIf { it.isNotBlank() }

    fun typeOrNull(): String? = type.trim().takeIf { it.isNotBlank() }

    fun dimensionOrNull(): String? = dimension.trim().takeIf { it.isNotBlank() }

    companion object {
        const val RESOURCE = "location"

        /**
         * The cache a location opened on its own is kept under (spec L2), as opposed to one
         * that arrived as part of a list.
         *
         * It is what makes the origin and last-location links on a character detail (spec
         * C3) work at all: those open a location no location list has necessarily ever
         * loaded. Like the other detail keys it is never written to `remote_keys` - it is not
         * a paged list and has no cursor - so the eviction that trims old searches cannot
         * reach it.
         */
        const val DETAIL = "$RESOURCE:detail"
    }
}
