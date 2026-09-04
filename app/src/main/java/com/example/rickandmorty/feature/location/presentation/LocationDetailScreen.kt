package com.example.rickandmorty.feature.location.presentation

import androidx.annotation.StringRes
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
import com.example.rickandmorty.feature.location.domain.model.LocationModel

/**
 * Spec S7. Stateless: everything it renders arrives as [uiState] and everything the user
 * does leaves as a [LocationDetailUiEvent].
 *
 * A `LazyColumn` for the same reason as the episode detail: the Citadel of Ricks lists a few
 * hundred residents, and composing that many avatars eagerly to put them in a scroll
 * container would cost far more than the screen is worth.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    uiState: LocationDetailUiState,
    onEvent: (LocationDetailUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // The name is the title once it is known; until then, a neutral one.
                        text = uiState.location?.name
                            ?: stringResource(R.string.location_detail_title),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onEvent(LocationDetailUiEvent.BackClicked) }) {
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
            isRefreshing = uiState.isRefreshing && uiState.location != null,
            onRefresh = { onEvent(LocationDetailUiEvent.Refreshed) },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isBlockingError -> ErrorState(
                    messageRes = checkNotNull(uiState.error).toMessageRes(),
                    onRetry = { onEvent(LocationDetailUiEvent.Refreshed) }
                )

                uiState.isLoading -> LoadingState()

                // Not null: isLoading and isBlockingError cover every null case above.
                else -> LocationDetailContent(
                    location = checkNotNull(uiState.location),
                    uiState = uiState,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun LocationDetailContent(
    location: LocationModel,
    uiState: LocationDetailUiState,
    onEvent: (LocationDetailUiEvent) -> Unit
) {
    // The heading counts the location's own resident ids rather than the loaded rows, so it
    // does not count up while the batch call is still landing.
    val residentCount = location.residentIds.size

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (uiState.error != null) {
            item(contentType = BANNER_CONTENT_TYPE) {
                ErrorBanner(
                    messageRes = uiState.error.toMessageRes(),
                    onRetry = { onEvent(LocationDetailUiEvent.Refreshed) }
                )
            }
        }

        item(contentType = SUMMARY_CONTENT_TYPE) {
            LocationSummary(location = location)
        }

        item(contentType = HEADING_CONTENT_TYPE) {
            Text(
                text = stringResource(R.string.location_detail_residents, residentCount),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
        }

        items(
            items = uiState.residents,
            key = { it.id },
            contentType = { RESIDENT_CONTENT_TYPE }
        ) { character ->
            CharacterRow(
                character = character,
                onClick = { onEvent(LocationDetailUiEvent.CharacterClicked(it)) }
            )
            HorizontalDivider()
        }

        // Only worth reporting when there is nothing cached to show instead.
        if (uiState.residents.isEmpty()) {
            item(contentType = MESSAGE_CONTENT_TYPE) {
                ResidentsPlaceholder(uiState = uiState, residentCount = residentCount)
            }
        }
    }
}

@Composable
private fun LocationSummary(location: LocationModel) {
    Column {
        Text(
            text = location.name,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp)
        )

        HorizontalDivider()

        // Both are `unknown` rather than absent for a good number of locations, and the API's
        // own wording is the honest thing to show.
        DetailRow(labelRes = R.string.detail_type, value = location.type)
        DetailRow(labelRes = R.string.location_detail_dimension, value = location.dimension)

        HorizontalDivider()
    }
}

@Composable
private fun ResidentsPlaceholder(uiState: LocationDetailUiState, residentCount: Int) {
    val modifier = Modifier.padding(horizontal = 16.dp)

    when {
        uiState.residentsError != null -> Text(
            text = stringResource(uiState.residentsError.toMessageRes()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            modifier = modifier
        )

        residentCount == 0 -> Text(
            text = stringResource(R.string.location_detail_residents_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )

        // The location has residents and nothing failed, so the batch call is still in flight.
        else -> LoadingState(modifier = Modifier.padding(24.dp))
    }
}

/** One labelled fact, matching the other two detail screens. */
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
private const val RESIDENT_CONTENT_TYPE = "resident"
private const val MESSAGE_CONTENT_TYPE = "message"
