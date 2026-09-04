package com.example.rickandmorty.feature.episode.domain.model

/**
 * An episode as the app's screens consume it.
 *
 * [code] is the API's `S01E01` string, which the detail screen shows as-is and the list
 * groups by (spec E1).
 */
data class EpisodeModel(
    val id: Int,
    val name: String,
    val airDate: String,
    val code: String,
    val characterIds: List<Int>,
    val url: String,
    val created: String
) {

    /**
     * The season this episode belongs to, read off [code] - the API has no season field, so
     * `S01E01` is the only thing that carries it.
     *
     * Null rather than a default when the code is not in that shape. The list would
     * otherwise file a malformed episode under season 1 alongside real ones, which is a
     * worse answer than admitting it does not know.
     */
    val season: Int?
        get() = code.uppercase()
            .substringAfter(SEASON_MARKER, missingDelimiterValue = "")
            .substringBefore(EPISODE_MARKER)
            .toIntOrNull()

    private companion object {
        const val SEASON_MARKER = "S"
        const val EPISODE_MARKER = "E"
    }
}
