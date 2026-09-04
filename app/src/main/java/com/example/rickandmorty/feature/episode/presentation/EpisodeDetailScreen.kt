package com.example.rickandmorty.feature.episode.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.rickandmorty.R
import com.example.rickandmorty.core.presentation.toMessageRes
import com.example.rickandmorty.core.ui.ErrorBanner
import com.example.rickandmorty.core.ui.ErrorState
import com.example.rickandmorty.core.ui.LoadingState
import com.example.rickandmorty.feature.character.presentation.CharacterRow
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel

/**
 * Spec S5. Stateless: everything it renders arrives as [uiState] and everything the user
 * does leaves as an [EpisodeDetailUiEvent].
 *
 * The three-way choice at the top is the one `PagedContent` makes for the lists and
 * `CharacterDetailScreen` makes for a character, for the same reason: a failed refresh may
 * not take cached content off screen.
 *
 * Unlike the character detail this is a `LazyColumn` rather than a scrolling `Column`. The
 * cast is the difference - an episode like *The Ricklantis Mixup* features over a hundred
 * characters, and composing that many avatars eagerly to put them in a scroll container
 * would cost far more than the screen is worth.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeDetailScreen(
    uiState: EpisodeDetailUiState,
    onEvent: (EpisodeDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // The name is the title once it is known; until then, a neutral one.
                        text = uiState.episode?.name
                            ?: stringResource(R.string.episode_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(EpisodeDetailUiEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            // With nothing cached the refresh is the blocking spinner below, not the indicator.
            isRefreshing = uiState.isRefreshing && uiState.episode != null,
            onRefresh = { onEvent(EpisodeDetailUiEvent.Refreshed) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isBlockingError -> ErrorState(
                    messageRes = checkNotNull(uiState.error).toMessageRes(),
                    onRetry = { onEvent(EpisodeDetailUiEvent.Refreshed) }
                )

                uiState.isLoading -> LoadingState()

                // Not null: isLoading and isBlockingError cover every null case above.
                else -> EpisodeDetailContent(
                    episode = checkNotNull(uiState.episode),
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun EpisodeDetailContent(
    episode: EpisodeModel,
    uiState: EpisodeDetailUiState,
    onEvent: (EpisodeDetailUiEvent) -> Unit
) {
    // The heading counts the episode's own character ids rather than the loaded rows, so it
    // does not count up while the cast is still arriving from the batch call.
    val castSize = episode.characterIds.size

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (uiState.error != null) {
            item(contentType = BANNER_CONTENT_TYPE) {
                ErrorBanner(
                    messageRes = uiState.error.toMessageRes(),
                    onRetry = { onEvent(EpisodeDetailUiEvent.Refreshed) }
                )
            }
        }

        item(contentType = SUMMARY_CONTENT_TYPE) {
            EpisodeSummary(episode = episode)
        }

        item(contentType = HEADING_CONTENT_TYPE) {
            Text(
                text = stringResource(R.string.episode_detail_cast, castSize),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(
            items = uiState.cast,
            key = { it.id },
            contentType = { CAST_CONTENT_TYPE }
        ) { character ->
            CharacterRow(
                character = character,
                onClick = { onEvent(EpisodeDetailUiEvent.CharacterClicked(it)) }
            )
            HorizontalDivider()
        }

        // Only worth reporting when there is nothing cached to show instead.
        if (uiState.cast.isEmpty()) {
            item(contentType = MESSAGE_CONTENT_TYPE) {
                CastPlaceholder(uiState = uiState, castSize = castSize)
            }
        }
    }
}

@Composable
private fun EpisodeSummary(episode: EpisodeModel) {
    Column {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = episode.name, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = episode.code,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        HorizontalDivider()

        DetailRow(labelRes = R.string.episode_detail_air_date, value = episode.airDate)
        DetailRow(
            labelRes = R.string.episode_detail_season,
            value = episode.season
                ?.let { stringResource(R.string.episodes_season, it) }
                ?: stringResource(R.string.episodes_season_unknown)
        )

        HorizontalDivider()
    }
}

@Composable
private fun CastPlaceholder(uiState: EpisodeDetailUiState, castSize: Int) {
    val modifier = Modifier.padding(horizontal = 16.dp)

    when {
        uiState.castError != null -> Text(
            text = stringResource(uiState.castError.toMessageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier
        )

        castSize == 0 -> Text(
            text = stringResource(R.string.episode_detail_cast_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )

        // The episode has a cast and nothing failed, so the batch call is still in flight.
        else -> LoadingState(modifier = Modifier.padding(24.dp))
    }
}

/** One labelled fact, matching the character detail's rows. */
@Composable
private fun DetailRow(@StringRes labelRes: Int, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private const val BANNER_CONTENT_TYPE = "banner"
private const val SUMMARY_CONTENT_TYPE = "summary"
private const val HEADING_CONTENT_TYPE = "heading"
private const val CAST_CONTENT_TYPE = "cast"
private const val MESSAGE_CONTENT_TYPE = "message"
