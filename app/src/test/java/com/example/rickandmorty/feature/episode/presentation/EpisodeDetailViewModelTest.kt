package com.example.rickandmorty.feature.episode.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.example.rickandmorty.core.common.DataError
import com.example.rickandmorty.core.common.Resource
import com.example.rickandmorty.feature.character.domain.model.CharacterLocationModel
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterOriginModel
import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import com.example.rickandmorty.feature.character.domain.usecase.GetCharactersByIdsUseCase
import com.example.rickandmorty.feature.character.domain.usecase.RefreshCharactersUseCase
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import com.example.rickandmorty.feature.episode.domain.usecase.ObserveEpisodeUseCase
import com.example.rickandmorty.feature.episode.domain.usecase.RefreshEpisodeUseCase
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Spec E2 and E4. The interesting part is that the cast is derived from the episode rather
 * than passed in - the ids are not known until the cache holds a copy.
 *
 * Robolectric because the episode id is read back off the type-safe route, which decodes
 * through a real Android `Bundle`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EpisodeDetailViewModelTest {

    private class FakeEpisodeRepository : EpisodeRepository {
        val cached = MutableStateFlow<EpisodeModel?>(null)
        var refreshResult: Resource<Unit> = Resource.Success(Unit)
        val refreshedIds = mutableListOf<Int>()

        override fun observeEpisode(id: Int): Flow<EpisodeModel?> = cached

        override suspend fun refreshEpisode(id: Int): Resource<Unit> {
            refreshedIds += id
            return refreshResult
        }

        override fun episodePaging(query: com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery) =
            error("not used")

        override fun observeEpisodes(ids: List<Int>) = error("not used")
        override suspend fun refreshEpisodes(ids: List<Int>) = error("not used")
    }

    private class FakeCharacterRepository : CharacterRepository {
        val cached = MutableStateFlow<List<CharacterModel>>(emptyList())
        var refreshResult: Resource<Unit> = Resource.Success(Unit)
        val refreshedIds = mutableListOf<List<Int>>()

        override fun observeCharactersByIds(ids: List<Int>): Flow<List<CharacterModel>> = cached

        override suspend fun refreshCharacters(ids: List<Int>): Resource<Unit> {
            refreshedIds += ids
            return refreshResult
        }

        override fun characterPaging(
            query: com.example.rickandmorty.feature.character.domain.model.CharacterQuery
        ) = error("not used")

        override fun observeCharacter(id: Int) = error("not used")
        override suspend fun refreshCharacter(id: Int) = error("not used")
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var episodes: FakeEpisodeRepository
    private lateinit var characters: FakeCharacterRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        episodes = FakeEpisodeRepository()
        characters = FakeCharacterRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(episodeId: Int = EPISODE_ID) = EpisodeDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("episodeId" to episodeId)),
        observeEpisode = ObserveEpisodeUseCase(episodes),
        getCharactersByIds = GetCharactersByIdsUseCase(characters),
        refreshEpisode = RefreshEpisodeUseCase(episodes),
        refreshCharacters = RefreshCharactersUseCase(characters)
    )

    @Test
    fun `refreshes the episode it was opened for`() = runTest(dispatcher) {
        viewModel()
        advanceUntilIdle()

        assertEquals(listOf(EPISODE_ID), episodes.refreshedIds)
    }

    @Test
    fun `shows the blocking spinner only until something is cached`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isLoading)

        episodes.cached.value = pilot
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Pilot", viewModel.uiState.value.episode?.name)
    }

    /** The offline rule: cached content wins over a failed refresh. */
    @Test
    fun `a failed refresh with a cached episode is a banner, not a blank screen`() =
        runTest(dispatcher) {
            episodes.cached.value = pilot
            episodes.refreshResult = Resource.Error(DataError.NoInternet)

            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(DataError.NoInternet, state.error)
            assertEquals("Pilot", state.episode?.name)
            assertFalse(state.isBlockingError)
        }

    @Test
    fun `a failed refresh with an empty cache is fatal`() = runTest(dispatcher) {
        episodes.refreshResult = Resource.Error(DataError.NoInternet)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isBlockingError)
    }

    /**
     * Spec E4: the cast ids come off the episode, so nothing is requested until the episode
     * itself is cached.
     */
    @Test
    fun `the cast is fetched for the ids the episode carries`() = runTest(dispatcher) {
        episodes.cached.value = pilot

        viewModel()
        advanceUntilIdle()

        assertTrue(characters.refreshedIds.contains(listOf(1, 2)))
    }

    @Test
    fun `an episode with no cast never asks for one`() = runTest(dispatcher) {
        episodes.cached.value = pilot.copy(characterIds = emptyList())

        viewModel()
        advanceUntilIdle()

        assertTrue(characters.refreshedIds.all { it.isEmpty() })
    }

    @Test
    fun `renders whatever the cast cache holds`() = runTest(dispatcher) {
        episodes.cached.value = pilot
        characters.cached.value = listOf(rick)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(rick), viewModel.uiState.value.cast)
    }

    /**
     * The two sections fail independently: the batch call for the cast going down is no
     * reason to take the episode off screen.
     */
    @Test
    fun `a failed cast fetch does not disturb the episode`() = runTest(dispatcher) {
        episodes.cached.value = pilot
        characters.refreshResult = Resource.Error(DataError.NoInternet)

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DataError.NoInternet, state.castError)
        assertEquals("Pilot", state.episode?.name)
        assertFalse(state.isBlockingError)
    }

    /** A cast cached before going offline stays on screen when the refresh fails. */
    @Test
    fun `a failed cast fetch leaves the cached cast on screen`() = runTest(dispatcher) {
        episodes.cached.value = pilot
        characters.cached.value = listOf(rick)
        characters.refreshResult = Resource.Error(DataError.NoInternet)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(rick), viewModel.uiState.value.cast)
    }

    @Test
    fun `pulling to refresh asks for the episode again`() = runTest(dispatcher) {
        episodes.cached.value = pilot
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(EpisodeDetailUiEvent.Refreshed)
        advanceUntilIdle()

        assertEquals(listOf(EPISODE_ID, EPISODE_ID), episodes.refreshedIds)
    }

    @Test
    fun `tapping a cast member asks for navigation exactly once`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(EpisodeDetailUiEvent.CharacterClicked(2))

            assertEquals(EpisodeDetailUiEffect.NavigateToCharacterDetail(2), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `back is an effect rather than something the screen decides`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(EpisodeDetailUiEvent.BackClicked)

            assertEquals(EpisodeDetailUiEffect.NavigateBack, awaitItem())
        }
    }

    private companion object {
        const val EPISODE_ID = 1

        val pilot = EpisodeModel(
            id = EPISODE_ID,
            name = "Pilot",
            airDate = "December 2, 2013",
            code = "S01E01",
            characterIds = listOf(1, 2),
            url = "https://rickandmortyapi.com/api/episode/1",
            created = "2017-11-10T12:56:33.798Z"
        )

        val rick = CharacterModel(
            id = 1,
            name = "Rick Sanchez",
            status = CharacterStatus.Alive,
            species = "Human",
            type = "",
            gender = Gender.Male,
            origin = CharacterOriginModel(name = "Earth (C-137)", id = 1),
            location = CharacterLocationModel(name = "Citadel of Ricks", id = 3),
            image = "",
            episodeIds = listOf(1),
            url = "",
            created = ""
        )
    }
}
