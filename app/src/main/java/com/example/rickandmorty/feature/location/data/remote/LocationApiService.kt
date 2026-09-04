package com.example.rickandmorty.feature.location.data.remote

import com.example.rickandmorty.feature.location.data.remote.dto.LocationDTO
import com.example.rickandmorty.feature.location.data.remote.dto.LocationInfoDTO
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface LocationApiService {

    /**
     * Pages are 1-based and 20 locations wide; there are 126 locations over 7 pages.
     *
     * The filters are the same endpoint, not a separate one, and combine freely (spec L3).
     * Retrofit omits a null [Query] entirely, so an unset filter never reaches the URL as an
     * empty parameter.
     *
     * A combination that matches nothing answers 404, not an empty list - see
     * `LocationRemoteMediator`.
     */
    @GET("location")
    suspend fun getLocationList(
        @Query("page") page: Int,
        @Query("name") name: String? = null,
        @Query("type") type: String? = null,
        @Query("dimension") dimension: String? = null
    ): LocationInfoDTO

    @GET("location/{id}")
    suspend fun getLocationById(@Path("id") id: Int): LocationDTO
}
