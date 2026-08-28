package com.example.rickandmorty.core.network

import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.core.common.Resource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * The one place in the app where a network exception is turned into a [Resource].
 *
 * Call it from repository implementations only; the Retrofit service keeps throwing
 * and neither use cases nor view models need a try/catch of their own.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): Resource<T> =
    try {
        Resource.Success(block())
    } catch (e: CancellationException) {
        // Coroutine cancellation is not a failure - it must keep propagating.
        throw e
    } catch (e: SocketTimeoutException) {
        Resource.Error(DataError.Timeout)
    } catch (e: IOException) {
        Resource.Error(DataError.NoInternet)
    } catch (e: HttpException) {
        Resource.Error(DataError.Http(e.code()))
    } catch (e: SerializationException) {
        Resource.Error(DataError.Serialization)
    } catch (e: Exception) {
        Resource.Error(DataError.Unknown(e))
    }
