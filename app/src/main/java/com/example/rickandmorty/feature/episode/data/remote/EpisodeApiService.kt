package com.example.rickandmorty.feature.episode.data.remote

import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path

interface EpisodeApiService {

    /**
     * One or many episodes - `episode/1` or `episode/1,2,3`.
     *
     * The return type is a raw [JsonElement] because the API answers the first with an
     * object and the second with an array (spec X5); `Json.decodeBatch` turns either into a
     * list. A `List<EpisodeDTO>` here would fail for every character that appears in exactly
     * one episode.
     */
    @GET("episode/{ids}")
    suspend fun getEpisodesByIds(@Path("ids") ids: String): JsonElement
}
