package com.example.rickandmorty.feature.character.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
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
 *
 * Search and filters live on this screen rather than a separate route: spec S2 is the same
 * screen as S1.
 */
@Composable
fun CharacterScreen(
    uiState: CharacterUiState,
    characters: LazyPagingItems<CharacterModel>,
    onEvent: (CharacterUiEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        CharacterFilterPanel(state = uiState, onEvent = onEvent)

        PagedContent(
            items = characters,
            // A search that found nothing reads differently from a list that is simply
            // empty, and after spec §8 the first is by far the more common of the two.
            emptyMessageRes = if (uiState.isSearching) {
                R.string.characters_empty_search
            } else {
                R.string.characters_empty
            }
        ) {
            CharacterList(
                characters = characters,
                onCharacterClick = { onEvent(CharacterUiEvent.CharacterClicked(it)) }
            )
        }
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
                CharacterRow(character = character, onClick = onCharacterClick)
                HorizontalDivider()
            }
        }

        pagingAppendFooter(characters)
    }
}

private const val CHARACTER_CONTENT_TYPE = "character"
