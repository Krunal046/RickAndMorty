package com.example.rickandmorty.feature.character.data.remote

import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface CharacterApiService {

    /** Pages are 1-based and 20 characters wide; the page size is fixed by the API. */
    @GET("character")
    suspend fun getCharacterList(@Query("page") page: Int): CharacterInfoDTO

    @GET("character/{id}")
    suspend fun getCharacterById(@Path("id")id: Int): CharacterDTO

    @GET("character/{ids}")
    suspend fun getSelectedCharacterList(@Path("ids")ids: String): List<CharacterDTO>

}
