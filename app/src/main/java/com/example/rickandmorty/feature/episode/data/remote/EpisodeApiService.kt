package com.example.rickandmorty.feature.episode.data.remote

import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeDTO
import com.example.rickandmorty.feature.episode.data.remote.dto.EpisodeInfoDTO
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface EpisodeApiService {

    /**
     * Pages are 1-based and 20 episodes wide; there are 51 episodes over 3 pages.
     *
     * `name` matches a title and `episode` matches the `S01E01` code - two parameters for
     * the one search box, routed by `EpisodeQuery`. Retrofit omits a null [Query] entirely,
     * so the unused one never reaches the URL.
     *
     * A search that matches nothing answers 404, not an empty list - see
     * `EpisodeRemoteMediator`.
     */
    @GET("episode")
    suspend fun getEpisodeList(
        @Query("page") page: Int,
        @Query("name") name: String? = null,
        @Query("episode") episode: String? = null
    ): EpisodeInfoDTO

    @GET("episode/{id}")
    suspend fun getEpisodeById(@Path("id") id: Int): EpisodeDTO

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
