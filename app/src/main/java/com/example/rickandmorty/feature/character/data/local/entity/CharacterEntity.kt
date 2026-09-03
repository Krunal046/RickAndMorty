package com.example.rickandmorty.feature.character.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A character as the database stores it.
 *
 * The API's nested `origin` and `location` objects are flattened into columns rather than
 * kept as embedded types: they are only ever a name plus a URL, and flat columns keep the
 * queries and the exported schema readable.
 *
 * [pageQuery] records which list this row was loaded for - `"character"` for the plain
 * list, `"character:name=beth&status=alive"` for a filtered one. One table therefore backs
 * every character list in the app, and a refresh of one query cannot disturb another's
 * rows. It is part of the primary key because the same character legitimately appears in
 * several lists at once, and the unfiltered list must not lose a row when a search that
 * also matched it is cleared.
 */
@Entity(tableName = "characters", primaryKeys = ["id", "pageQuery"])
data class CharacterEntity(
    val id: Int,
    val pageQuery: String,
    val name: String,
    val status: String,
    val species: String,
    val type: String,
    val gender: String,
    val originName: String,
    val originUrl: String,
    val lastLocationName: String,
    val lastLocationUrl: String,
    val imageUrl: String,
    val episodeIds: List<Int>,
    val url: String,
    val created: String,
    /**
     * Position within the list, assigned as pages arrive. Paging needs a stable ordering and
     * the API's own ordering is the one the user expects; sorting by id would reorder a
     * filtered list, and SQLite gives no guarantee of insertion order without it.
     */
    val orderInQuery: Int
)
