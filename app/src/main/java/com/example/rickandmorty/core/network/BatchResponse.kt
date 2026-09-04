package com.example.rickandmorty.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * Spec X5: the multi-id endpoints answer with a JSON **object** when the path carries one
 * id and an **array** when it carries several - `GET /episode/1` and `GET /episode/1,2` do
 * not have the same response shape.
 *
 * Declaring those endpoints as `List<T>` therefore breaks the moment a character appears in
 * exactly one episode, which is common. Every batch call in the app instead asks Retrofit
 * for the raw [JsonElement] and decodes it here, so no caller has to special-case a
 * one-element request and the rule is written down once.
 *
 * Call from inside `safeApiCall`, so a response that does not match [T] is reported as
 * `DataError.Serialization` like any other decoding failure.
 */
inline fun <reified T> Json.decodeBatch(payload: JsonElement): List<T> =
    if (payload is JsonArray) {
        payload.map { decodeFromJsonElement<T>(it) }
    } else {
        listOf(decodeFromJsonElement<T>(payload))
    }
