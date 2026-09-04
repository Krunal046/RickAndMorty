package com.example.rickandmorty.feature.episode.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.rickandmorty.R
import com.example.rickandmorty.core.ui.PagedContent
import com.example.rickandmorty.core.ui.pagingAppendFooter
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel

/**
 * Spec S4: the episode list, grouped by season, with the search box E3 asks for.
 *
 * Stateless - everything it renders arrives as a parameter and everything the user does
 * leaves as an [EpisodeUiEvent]. Loading, error and empty come from [PagedContent], which
 * reads them off `LazyPagingItems.loadState`.
 */
@Composable
fun EpisodeScreen(
    uiState: EpisodeUiState,
    episodes: LazyPagingItems<EpisodeListItem>,
    onEvent: (EpisodeUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        EpisodeSearchBar(uiState = uiState, onEvent = onEvent)

        PagedContent(
            items = episodes,
            // A search that found nothing reads differently from a list that is simply
            // empty, and after spec §8 the first is by far the more common of the two.
            emptyMessageRes = if (uiState.isSearching) {
                R.string.episodes_empty_search
            } else {
                R.string.episodes_empty
            }
        ) {
            EpisodeList(
                episodes = episodes,
                onEpisodeClick = { onEvent(EpisodeUiEvent.EpisodeClicked(it)) }
            )
        }
    }
}

/**
 * One box for both of the API's search parameters. The hint says a code is accepted, since
 * nothing else on screen would tell the user that typing `S02` is a thing that works.
 */
@Composable
private fun EpisodeSearchBar(
    uiState: EpisodeUiState,
    onEvent: (EpisodeUiEvent) -> Unit
) {
    OutlinedTextField(
        value = uiState.query.search,
        onValueChange = { onEvent(EpisodeUiEvent.SearchChanged(it)) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        singleLine = true,
        placeholder = { Text(stringResource(R.string.episodes_search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (uiState.query.search.isNotEmpty()) {
                IconButton(onClick = { onEvent(EpisodeUiEvent.SearchChanged("")) }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.action_clear_search)
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
    )
}

@Composable
private fun EpisodeList(
    episodes: LazyPagingItems<EpisodeListItem>,
    onEpisodeClick: (Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = episodes.itemCount,
            // Headings and episodes share one list, so the key has to distinguish them: a
            // season number and an episode id are both small integers.
            key = episodes.itemKey { item ->
                when (item) {
                    is EpisodeListItem.Header -> "season-${item.season}"
                    is EpisodeListItem.Item -> "episode-${item.episode.id}"
                }
            },
            contentType = episodes.itemContentType { item ->
                when (item) {
                    is EpisodeListItem.Header -> HEADER_CONTENT_TYPE
                    is EpisodeListItem.Item -> EPISODE_CONTENT_TYPE
                }
            }
        ) { index ->
            // Null only with placeholders enabled, which this list does not use.
            when (val item = episodes[index]) {
                is EpisodeListItem.Header -> SeasonHeader(season = item.season)

                is EpisodeListItem.Item -> {
                    EpisodeRow(episode = item.episode, onClick = onEpisodeClick)
                    HorizontalDivider()
                }

                null -> Unit
            }
        }

        pagingAppendFooter(episodes)
    }
}

/**
 * Not a sticky header: the list is short enough - 51 episodes over three pages - that one
 * pinned to the top would spend most of its time covering a row for no gain.
 */
@Composable
private fun SeasonHeader(season: Int?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = season
                ?.let { stringResource(R.string.episodes_season, it) }
                ?: stringResource(R.string.episodes_season_unknown),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun EpisodeRow(episode: EpisodeModel, onClick: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(episode.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = episode.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${episode.code} • ${episode.airDate}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private const val HEADER_CONTENT_TYPE = "season-header"
private const val EPISODE_CONTENT_TYPE = "episode"
