package com.example.rickandmorty.feature.location.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.rickandmorty.R
import com.example.rickandmorty.core.ui.PagedContent
import com.example.rickandmorty.core.ui.pagingAppendFooter
import com.example.rickandmorty.feature.location.domain.model.LocationModel

/**
 * Spec S6: the location list with the search and filters L3 asks for.
 *
 * Stateless - everything it renders arrives as a parameter and everything the user does
 * leaves as a [LocationUiEvent]. Loading, error and empty come from [PagedContent].
 */
@Composable
fun LocationScreen(
    uiState: LocationUiState,
    locations: LazyPagingItems<LocationModel>,
    onEvent: (LocationUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LocationFilterPanel(state = uiState, onEvent = onEvent)

        PagedContent(
            items = locations,
            // A search that found nothing reads differently from a list that is simply
            // empty, and after spec §8 the first is by far the more common of the two.
            emptyMessageRes = if (uiState.isSearching) {
                R.string.locations_empty_search
            } else {
                R.string.locations_empty
            }
        ) {
            LocationList(
                locations = locations,
                onLocationClick = { onEvent(LocationUiEvent.LocationClicked(it)) }
            )
        }
    }
}

@Composable
private fun LocationList(
    locations: LazyPagingItems<LocationModel>,
    onLocationClick: (Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = locations.itemCount,
            key = locations.itemKey { it.id },
            contentType = locations.itemContentType { LOCATION_CONTENT_TYPE }
        ) { index ->
            // Null only with placeholders enabled, which this list does not use.
            locations[index]?.let { location ->
                LocationRow(location = location, onClick = onLocationClick)
                HorizontalDivider()
            }
        }

        pagingAppendFooter(locations)
    }
}

@Composable
private fun LocationRow(location: LocationModel, onClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(location.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = location.name,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "${location.type} • ${location.dimension}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private const val LOCATION_CONTENT_TYPE = "location"
