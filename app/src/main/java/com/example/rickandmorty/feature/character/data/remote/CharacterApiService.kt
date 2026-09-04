package com.example.rickandmorty.feature.character.data.remote

import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CharacterApiService {

    /**
     * Pages are 1-based and 20 characters wide; the page size is fixed by the API.
     *
     * The filters are the same endpoint, not a separate one, and combine freely
     * (spec §5). Retrofit omits a null [Query] entirely, so an unset filter never
     * reaches the URL as an empty parameter.
     *
     * A combination that matches nothing answers 404, not an empty list - see
     * `CharacterRemoteMediator`.
     */
    @GET("character")
    suspend fun getCharacterList(
        @Query("page") page: Int,
        @Query("name") name: String? = null,
        @Query("status") status: String? = null,
        @Query("species") species: String? = null,
        @Query("gender") gender: String? = null
    ): CharacterInfoDTO

    @GET("character/{id}")
    suspend fun getCharacterById(@Path("id") id: Int): CharacterDTO

    /**
     * One or many characters - `character/1` or `character/1,2,3`.
     *
     * Raw [JsonElement] rather than `List<CharacterDTO>` because the API answers a single id
     * with an object and several with an array (spec X5); `Json.decodeBatch` turns either
     * into a list.
     */
    @GET("character/{ids}")
    suspend fun getCharactersByIds(@Path("ids") ids: String): JsonElement
}
