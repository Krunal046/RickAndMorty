package com.example.rickandmorty.feature.character.data.remote

import com.example.rickandmorty.feature.character.data.remote.dto.CharacterDTO
import com.example.rickandmorty.feature.character.data.remote.dto.CharacterInfoDTO
import retrofit2.http.GET
import retrofit2.http.Path

interface CharacterApiService {

    @GET("character?page=1")
    suspend fun getCharacterList(): CharacterInfoDTO

    @GET("character/{id}")
    suspend fun getCharacterById(@Path("id")id: Int): CharacterDTO

    @GET("character/{ids}")
    suspend fun getSelectedCharacterList(@Path("ids")ids: String): List<CharacterDTO>

}


