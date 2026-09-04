package com.example.rickandmorty.feature.location.data.local.entity

import androidx.room.Entity

/**
 * A location as the database stores it, shaped like `CharacterEntity` and `EpisodeEntity`
 * for the same reasons.
 *
 * [pageQuery] is part of the primary key because one table backs every location list, and
 * refreshing one must not disturb another's rows. The keys it takes - the plain list, a
 * filtered search, `LocationQuery.DETAIL` - are named by `LocationQuery`.
 */
@Entity(tableName = "locations", primaryKeys = ["id", "pageQuery"])
data class LocationEntity(
    val id: Int,
    val pageQuery: String,
    val name: String,
    val type: String,
    val dimension: String,
    val residentIds: List<Int>,
    val url: String,
    val created: String,
    /**
     * Position within the list, assigned as pages arrive. Paging needs a stable ordering and
     * the API's own is the one the user expects; sorting by id would reorder a filtered list.
     */
    val orderInQuery: Int
)
