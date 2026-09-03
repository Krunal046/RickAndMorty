package com.example.rickandmorty.core.common

/**
 * The API hands out related items as full URLs (`https://.../episode/28`) rather than ids,
 * so every feature has to read the id back off the tail before it can use the single or
 * multi-id endpoints.
 *
 * Returns null on anything that does not end in a number, so a malformed URL drops the one
 * relation instead of failing the whole parse.
 */
fun String.idFromUrlOrNull(): Int? = substringAfterLast('/').toIntOrNull()

/** Maps a list of resource URLs to the ids they point at, skipping any that are malformed. */
fun List<String>.idsFromUrls(): List<Int> = mapNotNull { it.idFromUrlOrNull() }
