package com.example.rickandmorty.feature.character.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.List
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
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender

/**
 * Spec S2's search box and filter chips, on the same screen as the list.
 *
 * The chip groups stay collapsed until asked for: three of them permanently open would take
 * most of a phone screen away from the list they filter. The badge on the toggle keeps the
 * active filters visible while they are hidden.
 */
@Composable
fun CharacterFilterPanel(
    state: CharacterUiState,
    onEvent: (CharacterUiEvent) -> Unit,
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
                onValueChange = { onEvent(CharacterUiEvent.SearchChanged(it)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.characters_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.query.name.isNotEmpty()) {
                        IconButton(onClick = { onEvent(CharacterUiEvent.SearchChanged("")) }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = stringResource(R.string.action_clear_search)
                            )
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = ImeAction.Search
                )
            )

            BadgedBox(
                badge = {
                    if (state.query.activeFilterCount > 0) {
                        Badge { Text(state.query.activeFilterCount.toString()) }
                    }
                }
            ) {
                IconButton(onClick = { onEvent(CharacterUiEvent.FiltersToggled) }) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = stringResource(R.string.action_filters)
                    )
                }
            }
        }

        if (state.filtersExpanded) {
            ChipGroup(
                labelRes = R.string.filter_status,
                options = CharacterStatus.entries,
                isSelected = { it == state.query.status },
                label = { it.name },
                onClick = { onEvent(CharacterUiEvent.StatusToggled(it)) }
            )

            ChipGroup(
                labelRes = R.string.filter_gender,
                options = Gender.entries,
                isSelected = { it == state.query.gender },
                label = { it.name },
                onClick = { onEvent(CharacterUiEvent.GenderToggled(it)) }
            )

            ChipGroup(
                labelRes = R.string.filter_species,
                options = SPECIES_OPTIONS,
                isSelected = { it == state.query.species },
                label = { it },
                onClick = { onEvent(CharacterUiEvent.SpeciesToggled(it)) }
            )

            if (state.query.activeFilterCount > 0) {
                TextButton(
                    onClick = { onEvent(CharacterUiEvent.FiltersCleared) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(stringResource(R.string.action_clear_filters))
                }
            }
        }
    }
}

@Composable
private fun <T> ChipGroup(
    labelRes: Int,
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onClick: (T) -> Unit
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
                    label = { Text(label(option)) }
                )
            }
        }
    }
}

/**
 * Species is free text on the API, so these are the values that actually occur in the
 * dataset rather than an enum. Anything not listed is still reachable by name search.
 */
private val SPECIES_OPTIONS = listOf(
    "Human",
    "Alien",
    "Humanoid",
    "Robot",
    "Animal",
    "Mythological Creature"
)
