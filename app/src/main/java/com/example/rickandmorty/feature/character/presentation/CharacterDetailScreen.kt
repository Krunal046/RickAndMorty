package com.example.rickandmorty.feature.character.presentation

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.AssistChip
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.rickandmorty.R
import com.example.rickandmorty.core.presentation.toMessageRes
import com.example.rickandmorty.core.ui.ErrorBanner
import com.example.rickandmorty.core.ui.ErrorState
import com.example.rickandmorty.core.ui.LoadingState
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel

/**
 * Spec S3. Stateless: everything it renders arrives as [uiState] and everything the user
 * does leaves as a [CharacterDetailUiEvent].
 *
 * The three-way choice at the top is the same one `PagedContent` makes for the lists, and
 * for the same reason: the screen reads the database, a refresh is a background attempt to
 * update it, and a failed refresh may not take cached content off screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterDetailScreen(
    uiState: CharacterDetailUiState,
    onEvent: (CharacterDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // The name is the title once it is known; until then, a neutral one.
                        text = uiState.character?.name
                            ?: stringResource(R.string.character_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CharacterDetailUiEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    // Spec X3. Hidden until a character is on screen: there is nothing to
                    // save while the cache is still empty, and a heart over a spinner
                    // invites a tap that would do nothing.
                    if (uiState.character != null) {
                        IconButton(
                            onClick = { onEvent(CharacterDetailUiEvent.FavoriteToggled) }
                        ) {
                            Icon(
                                imageVector = if (uiState.isFavorite) {
                                    Icons.Filled.Favorite
                                } else {
                                    Icons.Filled.FavoriteBorder
                                },
                                contentDescription = stringResource(
                                    if (uiState.isFavorite) {
                                        R.string.action_unfavorite
                                    } else {
                                        R.string.action_favorite
                                    }
                                ),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            // With nothing cached the refresh is the blocking spinner below, not the indicator.
            isRefreshing = uiState.isRefreshing && uiState.character != null,
            onRefresh = { onEvent(CharacterDetailUiEvent.Refreshed) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isBlockingError -> ErrorState(
                    messageRes = checkNotNull(uiState.error).toMessageRes(),
                    onRetry = { onEvent(CharacterDetailUiEvent.Refreshed) }
                )

                uiState.isLoading -> LoadingState()

                // Not null: isLoading and isBlockingError cover every null case above.
                else -> CharacterDetailContent(
                    character = checkNotNull(uiState.character),
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun CharacterDetailContent(
    character: CharacterModel,
    uiState: CharacterDetailUiState,
    onEvent: (CharacterDetailUiEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            // Scrollable so a pull is available even when the content is short.
            .verticalScroll(rememberScrollState())
    ) {
        if (uiState.error != null) {
            ErrorBanner(
                messageRes = uiState.error.toMessageRes(),
                onRetry = { onEvent(CharacterDetailUiEvent.Refreshed) }
            )
        }

        AsyncImage(
            model = character.image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = character.name, style = MaterialTheme.typography.headlineSmall)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusDot(status = character.status)
                Text(
                    text = "${character.status} • ${character.species}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        HorizontalDivider()

        DetailRow(labelRes = R.string.detail_gender, value = character.gender.name)
        DetailRow(
            labelRes = R.string.detail_type,
            // Type is blank for most characters rather than absent.
            value = character.type.ifBlank { stringResource(R.string.detail_value_unknown) }
        )

        // Origin and last location open the location detail (spec S7) - but only when the
        // API gave one. An `unknown` origin has no url and so no id, and a link that leads
        // nowhere is worse than plain text.
        val onOriginClick: (() -> Unit)? = character.origin.id?.let { locationId ->
            { onEvent(CharacterDetailUiEvent.LocationClicked(locationId)) }
        }
        val onLocationClick: (() -> Unit)? = character.location.id?.let { locationId ->
            { onEvent(CharacterDetailUiEvent.LocationClicked(locationId)) }
        }

        DetailRow(
            labelRes = R.string.detail_origin,
            value = character.origin.name,
            onClick = onOriginClick
        )
        DetailRow(
            labelRes = R.string.detail_last_location,
            value = character.location.name,
            onClick = onLocationClick
        )

        HorizontalDivider()

        EpisodeSection(uiState = uiState, onEvent = onEvent)
    }
}

/**
 * Spec C4. The chips are driven by what the cache holds, so they fill in as the batch call
 * lands and stay put offline.
 *
 * The count comes from the character's own episode ids rather than the loaded chips, so the
 * heading does not count down while the episodes are still arriving.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EpisodeSection(
    uiState: CharacterDetailUiState,
    onEvent: (CharacterDetailUiEvent) -> Unit
) {
    val episodeCount = uiState.character?.episodeIds?.size ?: 0

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.detail_episodes, episodeCount),
            style = MaterialTheme.typography.titleMedium
        )

        when {
            uiState.episodes.isNotEmpty() -> FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.episodes.forEach { episode ->
                    EpisodeChip(
                        episode = episode,
                        onClick = { onEvent(CharacterDetailUiEvent.EpisodeClicked(episode.id)) }
                    )
                }
            }

            // Only worth reporting when there is nothing cached to show instead.
            uiState.episodesError != null -> Text(
                text = stringResource(uiState.episodesError.toMessageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )

            episodeCount == 0 -> Text(
                text = stringResource(R.string.detail_episodes_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EpisodeChip(episode: EpisodeModel, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = "${episode.code} · ${episode.name}") }
    )
}

/**
 * One labelled fact. Tappable only when [onClick] is given, so a row that leads nowhere does
 * not look like it does.
 */
@Composable
private fun DetailRow(
    @StringRes labelRes: Int,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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

        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
