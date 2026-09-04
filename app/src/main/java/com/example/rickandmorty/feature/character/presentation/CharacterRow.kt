package com.example.rickandmorty.feature.character.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import coil3.compose.AsyncImage
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterStatus

/**
 * One character as a list row: avatar, name, status dot, species and last location.
 *
 * Lifted out of the character list because the same card is what the favorites tab renders,
 * and what an episode's cast and a location's residents will render later - spec E4 and L4
 * both say "reuses the character card". Two lists drawing a character two different ways is
 * the thing this prevents.
 *
 * [trailing] is the slot that lets a screen add its own affordance - the favorites tab hangs
 * its remove button there - without the row learning what any one screen wants.
 */
@Composable
internal fun CharacterRow(
    character: CharacterModel,
    onClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick(character.id) }
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

        trailing?.invoke()
    }
}

/**
 * Exhaustive over [CharacterStatus] now that the API's free text is parsed into an enum at
 * the data boundary - adding a status would fail to compile here rather than silently
 * rendering the neutral colour.
 */
@Composable
internal fun StatusDot(status: CharacterStatus) {
    val color = when (status) {
        CharacterStatus.Alive -> Color(0xFF4CAF50)
        CharacterStatus.Dead -> Color(0xFFE53935)
        CharacterStatus.Unknown -> MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}
