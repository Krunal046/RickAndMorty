package com.example.rickandmorty.feature.character.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.model.CharacterLocationModel
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterOriginModel
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import com.example.rickandmorty.feature.character.domain.usecase.ObserveCharacterUseCase
import com.example.rickandmorty.feature.character.domain.usecase.RefreshCharacterUseCase
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import com.example.rickandmorty.feature.episode.domain.usecase.GetEpisodesByIdsUseCase
import com.example.rickandmorty.feature.episode.domain.usecase.RefreshEpisodesUseCase
import com.example.rickandmorty.feature.favorite.domain.repository.FavoriteRepository
import com.example.rickandmorty.feature.favorite.domain.usecase.ObserveIsFavoriteUseCase
import com.example.rickandmorty.feature.favorite.domain.usecase.ToggleFavoriteUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Robolectric because the character id is read back off the type-safe route, which decodes
 * through a `SavedState` and so needs an Android runtime.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CharacterDetailViewModelTest {

    private class FakeCharacterRepository : CharacterRepository {
        val cached = MutableStateFlow<CharacterModel?>(null)
        var refreshResult: Resource<Unit> = Resource.Success(Unit)
        val refreshedIds = mutableListOf<Int>()

        override fun characterPaging(query: CharacterQuery) = error("not used")

        override fun observeCharacter(id: Int): Flow<CharacterModel?> = cached

        override suspend fun refreshCharacter(id: Int): Resource<Unit> {
            refreshedIds += id
            return refreshResult
        }

        override fun observeCharactersByIds(ids: List<Int>) = error("not used")

        override suspend fun refreshCharacters(ids: List<Int>) = error("not used")
    }

    private class FakeEpisodeRepository : EpisodeRepository {
        val cached = MutableStateFlow<List<EpisodeModel>>(emptyList())
        var refreshResult: Resource<Unit> = Resource.Success(Unit)
        val refreshedIds = mutableListOf<List<Int>>()

        override fun observeEpisodes(ids: List<Int>): Flow<List<EpisodeModel>> = cached

        override suspend fun refreshEpisodes(ids: List<Int>): Resource<Unit> {
            refreshedIds += ids
            return refreshResult
        }

        override fun episodePaging(query: EpisodeQuery) = error("not used")

        override fun observeEpisode(id: Int) = error("not used")

        override suspend fun refreshEpisode(id: Int) = error("not used")
    }

    private class FakeFavoriteRepository : FavoriteRepository {
        val saved = MutableStateFlow(false)
        val toggled = mutableListOf<Int>()

        override fun observeFavorites() = error("not used")

        override fun observeIsFavorite(characterId: Int): Flow<Boolean> = saved

        override suspend fun toggleFavorite(characterId: Int) {
            toggled += characterId
            saved.value = !saved.value
        }
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var characters: FakeCharacterRepository
    private lateinit var episodes: FakeEpisodeRepository
    private lateinit var favorites: FakeFavoriteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        characters = FakeCharacterRepository()
        episodes = FakeEpisodeRepository()
        favorites = FakeFavoriteRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(characterId: Int = CHARACTER_ID) = CharacterDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("characterId" to characterId)),
        observeCharacter = ObserveCharacterUseCase(characters),
        getEpisodesByIds = GetEpisodesByIdsUseCase(episodes),
        observeIsFavorite = ObserveIsFavoriteUseCase(favorites),
        refreshCharacter = RefreshCharacterUseCase(characters),
        refreshEpisodes = RefreshEpisodesUseCase(episodes),
        toggleFavorite = ToggleFavoriteUseCase(favorites)
    )

    @Test
    fun `refreshes the character it was opened for`() = runTest(dispatcher) {
        viewModel()
        advanceUntilIdle()

        assertEquals(listOf(CHARACTER_ID), characters.refreshedIds)
    }

    @Test
    fun `shows the blocking spinner only until something is cached`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isLoading)

        characters.cached.value = rick
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Rick Sanchez", viewModel.uiState.value.character?.name)
    }

    /** The offline rule: cached content wins over a failed refresh. */
    @Test
    fun `a failed refresh with a cached character is a banner, not a blank screen`() =
        runTest(dispatcher) {
            characters.cached.value = rick
            characters.refreshResult = Resource.Error(DataError.NoInternet)

            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(DataError.NoInternet, state.error)
            assertEquals("Rick Sanchez", state.character?.name)
            assertFalse(state.isBlockingError)
        }

    @Test
    fun `a failed refresh with an empty cache is fatal`() = runTest(dispatcher) {
        characters.refreshResult = Resource.Error(DataError.NoInternet)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isBlockingError)
    }

    @Test
    fun `a successful refresh clears a previous failure`() = runTest(dispatcher) {
        characters.cached.value = rick
        characters.refreshResult = Resource.Error(DataError.Timeout)

        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(DataError.Timeout, viewModel.uiState.value.error)

        characters.refreshResult = Resource.Success(Unit)
        viewModel.onEvent(CharacterDetailUiEvent.Refreshed)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.error)
    }

    /** Spec C4: the episodes to fetch come from the character, so only after it has loaded. */
    @Test
    fun `fetches the episodes the character turns out to appear in`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        characters.cached.value = rick
        episodes.cached.value = listOf(pilot)
        advanceUntilIdle()

        assertTrue(episodes.refreshedIds.contains(listOf(1, 2)))
        assertEquals(listOf(pilot), viewModel.uiState.value.episodes)
    }

    /** The episode chips failing is no reason to take the character off screen. */
    @Test
    fun `an episode batch failure is reported separately from the character`() =
        runTest(dispatcher) {
            characters.cached.value = rick
            episodes.refreshResult = Resource.Error(DataError.Timeout)

            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(DataError.Timeout, state.episodesError)
            assertNull(state.error)
            assertEquals("Rick Sanchez", state.character?.name)
        }

    @Test
    fun `tapping an episode chip asks for navigation exactly once`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(CharacterDetailUiEvent.EpisodeClicked(28))

            assertEquals(CharacterDetailUiEffect.NavigateToEpisode(28), awaitItem())
            expectNoEvents()
        }
    }

    /** Origin and last known location both open the location detail (spec S7). */
    @Test
    fun `tapping a location asks to open the location detail`() = runTest(dispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.effects.test {
            viewModel.onEvent(CharacterDetailUiEvent.LocationClicked(3))

            assertEquals(CharacterDetailUiEffect.NavigateToLocation(3), awaitItem())
        }
    }

    /** Spec X3: the heart reflects the favorites table, not a flag the screen keeps. */
    @Test
    fun `the heart follows the saved state rather than the tap`() = runTest(dispatcher) {
        characters.cached.value = rick
        val viewModel = viewModel()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFavorite)

        viewModel.onEvent(CharacterDetailUiEvent.FavoriteToggled)
        advanceUntilIdle()

        assertEquals(listOf(CHARACTER_ID), favorites.toggled)
        assertTrue(viewModel.uiState.value.isFavorite)
    }

    @Test
    fun `tapping the heart again removes the favorite`() = runTest(dispatcher) {
        characters.cached.value = rick
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(CharacterDetailUiEvent.FavoriteToggled)
        advanceUntilIdle()
        viewModel.onEvent(CharacterDetailUiEvent.FavoriteToggled)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isFavorite)
    }

    private companion object {
        const val CHARACTER_ID = 1

        val rick = CharacterModel(
            id = CHARACTER_ID,
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

        val pilot = EpisodeModel(
            id = 1,
            name = "Pilot",
            airDate = "December 2, 2013",
            code = "S01E01",
            characterIds = listOf(1, 2),
            url = "https://rickandmortyapi.com/api/episode/1",
            created = "2017-11-10T12:56:33.798Z"
        )
    }
}
