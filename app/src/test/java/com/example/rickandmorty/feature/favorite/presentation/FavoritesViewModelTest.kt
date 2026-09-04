package com.example.rickandmorty.feature.favorite.presentation

import app.cash.turbine.test
import com.example.rickandmorty.feature.character.domain.model.CharacterLocationModel
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterOriginModel
import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender
import com.example.rickandmorty.feature.favorite.domain.repository.FavoriteRepository
import com.example.rickandmorty.feature.favorite.domain.usecase.ObserveFavoriteCharactersUseCase
import com.example.rickandmorty.feature.favorite.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private class FakeFavoriteRepository : FavoriteRepository {
        val saved = MutableStateFlow<List<CharacterModel>>(emptyList())
        val toggled = mutableListOf<Int>()

        override fun observeFavorites(): Flow<List<CharacterModel>> = saved

        override fun observeIsFavorite(characterId: Int): Flow<Boolean> = flowOf(false)

        override suspend fun toggleFavorite(characterId: Int) {
            toggled += characterId
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeFavoriteRepository
    private lateinit var viewModel: FavoritesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeFavoriteRepository()
        viewModel = FavoritesViewModel(
            observeFavoriteCharacters = ObserveFavoriteCharactersUseCase(repository),
            toggleFavorite = ToggleFavoriteUseCase(repository)
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    /** Without this the tab would flash "no favorites" before Room had answered. */
    @Test
    fun `starts loading rather than empty`() = runTest(dispatcher) {
        assertTrue(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `an empty database is an empty tab, not a loading one`() = runTest(dispatcher) {
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun `renders whatever the database holds`() = runTest(dispatcher) {
        repository.saved.value = listOf(rick)
        advanceUntilIdle()

        assertEquals(listOf(rick), viewModel.uiState.value.characters)
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    /** The list follows the database rather than being edited in place. */
    @Test
    fun `removing a favorite goes through the same toggle the heart uses`() =
        runTest(dispatcher) {
            repository.saved.value = listOf(rick)
            advanceUntilIdle()

            viewModel.onEvent(FavoritesUiEvent.FavoriteToggled(rick.id))
            advanceUntilIdle()

            assertEquals(listOf(rick.id), repository.toggled)
        }

    @Test
    fun `tapping a favorite asks for navigation exactly once`() = runTest(dispatcher) {
        viewModel.effects.test {
            viewModel.onEvent(FavoritesUiEvent.CharacterClicked(42))

            assertEquals(FavoritesUiEffect.NavigateToCharacterDetail(42), awaitItem())
            expectNoEvents()
        }
    }

    private companion object {
        val rick = CharacterModel(
            id = 1,
            name = "Rick Sanchez",
            status = CharacterStatus.Alive,
            species = "Human",
            type = "",
            gender = Gender.Male,
            origin = CharacterOriginModel(name = "Earth (C-137)", id = 1),
            location = CharacterLocationModel(name = "Citadel of Ricks", id = 3),
            image = "https://rickandmortyapi.com/api/character/avatar/1.jpeg",
            episodeIds = listOf(1, 2),
            url = "https://rickandmortyapi.com/api/character/1",
            created = "2017-11-04T18:48:46.250Z"
        )
    }
}
