package dev.datlag.mimasu.ui.navigation.screen.initial.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import com.vanniktech.locale.Locale
import com.vanniktech.locale.Locales
import dev.datlag.mimasu.common.default
import dev.datlag.mimasu.common.localized
import dev.datlag.mimasu.tmdb.api.Trending
import dev.datlag.mimasu.tmdb.model.TrendingWindow
import dev.datlag.tooling.compose.platform.PlatformButton
import dev.datlag.tooling.compose.platform.PlatformText
import dev.datlag.tooling.decompose.lifecycle.collectAsStateWithLifecycle
import io.github.aakira.napier.Napier

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(component: HomeComponent) {
    val trendingAll by component.trendingMovies.collectAsStateWithLifecycle(null)
    val trendingPeople by component.trendingPeople.collectAsStateWithLifecycle(null)

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stickyHeader {
            Text(text = "All may include movies, tv and people")
        }
        trendingAll?.let {
            items(it.results.toList()) { t ->
                Text(text = t.mediaType ?: t.id.toString())
            }
        }
        stickyHeader {
            Text(text = "People only")
        }
        trendingPeople?.let {
            items(it.results.toList()) { t ->
                Text(text = t.mediaType ?: t.id.toString())

                LaunchedEffect(Unit) {
                    if (t is Trending.Response.Media.Person) {
                        Napier.e(t.profilePicture ?: t.name)
                    }
                }
            }
        }
    }
}