package com.example.rickandmorty.feature.favorite.data.repository

import androidx.room.withTransaction
import com.example.rickandmorty.core.database.RickAndMortyDatabase
import com.example.rickandmorty.feature.character.data.local.dao.CharacterDao
import com.example.rickandmorty.feature.character.data.mapper.toDomain
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.favorite.data.local.dao.FavoriteDao
import com.example.rickandmorty.feature.favorite.data.local.entity.FavoriteCharacterEntity
import com.example.rickandmorty.feature.favorite.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val characterDao: CharacterDao,
    private val database: RickAndMortyDatabase
) : FavoriteRepository {

    override fun observeFavorites(): Flow<List<CharacterModel>> =
        favoriteDao.observeFavoriteCharacters().map { rows -> rows.map { it.toDomain() } }

    override fun observeIsFavorite(characterId: Int): Flow<Boolean> =
        favoriteDao.observeIsFavorite(characterId)

    /**
     * Toggling is one transaction over two tables, because a favorite is only half the
     * record: the tab renders characters, and the row it renders has to survive.
     *
     * Saving therefore pins a copy of the character under [CharacterQuery.FAVORITE] as well
     * as recording the id. Without it the favorite would rest on cache - a row a list
     * refresh clears before writing its new page, or that the eviction of an old search
     * drops - and a character saved from a search would disappear from the tab as soon as
     * that search aged out. Removing releases the pin again, so an unfavorited character
     * costs nothing.
     *
     * The rule of what a tap means lives here rather than in the ViewModel so that every
     * caller - the detail screen's heart and the tab's remove button - agrees on it.
     */
    override suspend fun toggleFavorite(characterId: Int) {
        database.withTransaction {
            if (favoriteDao.isFavorite(characterId)) {
                favoriteDao.delete(characterId)
                characterDao.clearRow(id = characterId, pageQuery = CharacterQuery.FAVORITE)
            } else {
                pinCharacter(characterId)
                favoriteDao.upsert(
                    FavoriteCharacterEntity(
                        characterId = characterId,
                        favoritedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    /**
     * Copies whatever the cache holds for this character into the pinned row.
     *
     * The detail copy is preferred because the screen the heart lives on has just refreshed
     * it; any other copy is an equally good fallback, since a refresh rewrites every copy of
     * a character together. A character with nothing cached cannot be favorited from
     * anywhere in the app, so there is nothing to pin and nothing to do.
     */
    private suspend fun pinCharacter(characterId: Int) {
        val cached = characterDao.rowsForId(characterId)
        val source = cached.firstOrNull { it.pageQuery == CharacterQuery.DETAIL }
            ?: cached.firstOrNull()
            ?: return

        characterDao.upsertAll(
            listOf(source.copy(pageQuery = CharacterQuery.FAVORITE, orderInQuery = 0))
        )
    }
}
