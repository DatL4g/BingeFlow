package dev.datlag.mimasu.tmdb.repository

import com.mayakapps.kache.InMemoryKache
import com.mayakapps.kache.KacheStrategy
import dev.datlag.mimasu.tmdb.api.Details
import dev.datlag.mimasu.tmdb.model.DetailState
import dev.datlag.sekret.Secret
import dev.datlag.tooling.async.suspendCatching
import io.ktor.client.call.body
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.days

data class DetailsRepository(
    @Secret private val apiKey: String,
    private val details: Details,
    private val language: String
) {

    private val movieKache = InMemoryKache<Int, Details.Movie>(
        maxSize = 5 * 1024 * 1024
    ) {
        strategy = KacheStrategy.LRU
        expireAfterWriteDuration = 1.days
    }

    suspend fun load(
        id: Int
    ): DetailState<Details.Movie> {
        suspend fun response() = suspendCatching {
            val response = details.movie(
                apiKey = apiKey,
                id = id,
                language = language,
                appendToResponse = "videos,credits"
            )

            response.body<Details.Movie>()
        }

        val result = suspendCatching {
            movieKache.getOrPut(id) {
                response().getOrThrow()
            }
        }

        return (result.getOrNull() ?: response().getOrNull())?.let {
            DetailState.Success(it)
        } ?: DetailState.Error(result.exceptionOrNull())
    }
}