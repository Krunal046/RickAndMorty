package com.example.rickandmorty.feature.location.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.rickandmorty.R

/**
 * Spec L3's search box and filter chips, on the same screen as the list - the shape spec S2
 * established for characters, so the two lists are filtered the same way.
 *
 * The chip groups stay collapsed until asked for, and the badge on the toggle keeps the
 * active filters visible while they are hidden.
 */
@Composable
fun LocationFilterPanel(
    state: LocationUiState,
    onEvent: (LocationUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            OutlinedTextField(
                value = state.query.name,
                onValueChange = { onEvent(LocationUiEvent.SearchChanged(it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.locations_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.name.isNotEmpty()) {
                        IconButton(onClick = { onEvent(LocationUiEvent.SearchChanged("")) }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.action_clear_search)
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            BadgedBox(
                badge = {
                    if (state.query.activeFilterCount > 0) {
                        Badge { Text(state.query.activeFilterCount.toString()) }
                    }
                }
            ) {
                IconButton(onClick = { onEvent(LocationUiEvent.FiltersToggled) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = stringResource(R.string.action_filters)
                    )
                }
            }
        }

        if (state.filtersExpanded) {
            ChipGroup(
                labelRes = R.string.filter_type,
                options = TYPE_OPTIONS,
                isSelected = { it == state.query.type },
                onClick = { onEvent(LocationUiEvent.TypeToggled(it)) }
            )

            ChipGroup(
                labelRes = R.string.filter_dimension,
                options = DIMENSION_OPTIONS,
                isSelected = { it == state.query.dimension },
                onClick = { onEvent(LocationUiEvent.DimensionToggled(it)) }
            )

            if (state.query.activeFilterCount > 0) {
                TextButton(
                    onClick = { onEvent(LocationUiEvent.FiltersCleared) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.action_clear_filters))
                }
            }
        }
    }
}

@Composable
private fun ChipGroup(
    labelRes: Int,
    options: List<String>,
    isSelected: (String) -> Boolean,
    onClick: (String) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 4.dp)) {
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = isSelected(option),
                    onClick = { onClick(option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

/**
 * Type and dimension are free text on the API, exactly as species is for characters, so
 * these are the values that actually occur in the dataset rather than an enum. The API
 * matches them as substrings, so anything not listed is still reachable by name search.
 */
private val TYPE_OPTIONS = listOf(
    "Planet",
    "Space station",
    "Microverse",
    "Cluster",
    "Resort",
    "TV",
    "Dream"
)

private val DIMENSION_OPTIONS = listOf(
    "Dimension C-137",
    "Replacement Dimension",
    "Post-Apocalyptic Dimension",
    "Cronenberg Dimension",
    "Fantasy Dimension",
    "unknown"
)
