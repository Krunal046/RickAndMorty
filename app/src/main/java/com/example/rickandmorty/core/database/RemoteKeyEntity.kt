package com.example.rickandmorty.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The paging cursor for one list, shared by every feature.
 *
 * [queryKey] identifies the list a cursor belongs to, including its filters -
 * `"character"`, `"character:status=alive&name=beth"`, `"location:type=Planet"`. Keying on
 * the query rather than the resource is what lets a filtered list be cached and paged
 * offline like any other, instead of only the unfiltered one.
 */
@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val queryKey: String,
    /** Page to request next, or null once the API reported `info.next == null`. */
    val nextPage: Int?,
    val lastUpdated: Long
)
