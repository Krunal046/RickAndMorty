package com.example.rickandmorty.feature.character.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil3.compose.AsyncImage
import com.example.rickandmorty.R
import com.example.rickandmorty.core.ui.PagedContent
import com.example.rickandmorty.core.ui.pagingAppendFooter
import com.example.rickandmorty.feature.character.domain.model.CharacterModel

/**
 * Stateless: everything it renders arrives as a parameter, and everything the user does
 * leaves as a [CharacterUiEvent].
 *
 * Loading, error and empty are not parameters either - [PagedContent] derives them from
 * `LazyPagingItems.loadState`, the single source for them.
 */
@Composable
fun CharacterScreen(
    characters: LazyPagingItems<CharacterModel>,
    onEvent: (CharacterUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    PagedContent(
        items = characters,
        emptyMessageRes = R.string.characters_empty,
        modifier = modifier
    ) {
        CharacterList(
            characters = characters,
            onCharacterClick = { onEvent(CharacterUiEvent.CharacterClicked(it)) }
        )
    }
}

@Composable
private fun CharacterList(
    characters: LazyPagingItems<CharacterModel>,
    onCharacterClick: (Int) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(
            count = characters.itemCount,
            key = characters.itemKey { it.id },
            contentType = characters.itemContentType { CHARACTER_CONTENT_TYPE }
        ) { index ->
            // Null only with placeholders enabled, which this list does not use.
            characters[index]?.let { character ->
                CharacterRow(character = character, onCharacterClick = onCharacterClick)
                HorizontalDivider()
            }
        }

        pagingAppendFooter(characters)
    }
}

@Composable
private fun CharacterRow(
    character: CharacterModel,
    onCharacterClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCharacterClick(character.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AsyncImage(
            model = character.image,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = character.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                StatusDot(status = character.status)
                Text(
                    text = "${character.status} • ${character.species}",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = character.location.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The API sends status as free text (`Alive`/`Dead`/`unknown`) with inconsistent casing,
 * so the comparison is case-insensitive and anything unrecognised falls back to the
 * neutral colour rather than being treated as a failure.
 */
@Composable
private fun StatusDot(status: String) {
    val color = when (status.lowercase()) {
        "alive" -> Color(0xFF4CAF50)
        "dead" -> Color(0xFFE53935)
        else -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private const val CHARACTER_CONTENT_TYPE = "character"
