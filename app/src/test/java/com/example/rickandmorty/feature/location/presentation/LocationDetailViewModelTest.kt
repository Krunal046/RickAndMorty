package com.example.rickandmorty.feature.location.presentation

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
import com.example.rickandmorty.feature.character.domain.usecase.GetCharactersByIdsUseCase
import com.example.rickandmorty.feature.character.domain.usecase.RefreshCharactersUseCase
import com.example.rickandmorty.feature.location.domain.model.LocationModel
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import com.example.rickandmorty.feature.location.domain.repository.LocationRepository
import com.example.rickandmorty.feature.location.domain.usecase.ObserveLocationUseCase
import com.example.rickandmorty.feature.location.domain.usecase.RefreshLocationUseCase
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
 * Spec L2 and L4.
 *
 * Robolectric because the location id is read back off the type-safe route, which decodes
 * through a real Android `Bundle`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationDetailViewModelTest {

    private class FakeLocationRepository : LocationRepository {
        val cached = MutableStateFlow<LocationModel?>(null)
        var refreshResult: Resource<Unit> = Resource.Success(Unit)
        val refreshedIds = mutableListOf<Int>()

        override fun observeLocation(id: Int): Flow<LocationModel?> = cached

        override suspend fun refreshLocation(id: Int): Resource<Unit> {
            refreshedIds += id
            return refreshResult
        }

        override fun locationPaging(query: LocationQuery) = error("not used")
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

        override fun characterPaging(query: CharacterQuery) = error("not used")
        override fun observeCharacter(id: Int) = error("not used")
        override suspend fun refreshCharacter(id: Int) = error("not used")
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var locations: FakeLocationRepository
    private lateinit var characters: FakeCharacterRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        locations = FakeLocationRepository()
        characters = FakeCharacterRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(locationId: Int = LOCATION_ID) = LocationDetailViewModel(
        savedStateHandle = SavedStateHandle(mapOf("locationId" to locationId)),
        observeLocation = ObserveLocationUseCase(locations),
        getCharactersByIds = GetCharactersByIdsUseCase(characters),
        refreshLocation = RefreshLocationUseCase(locations),
        refreshCharacters = RefreshCharactersUseCase(characters)
    )

    @Test
    fun `refreshes the location it was opened for`() = runTest(dispatcher) {
        viewModel()
        advanceUntilIdle()

        assertEquals(listOf(LOCATION_ID), locations.refreshedIds)
    }

    /**
     * The case a character's origin link produces: nothing is cached, so the screen has to
     * fetch before it has anything to show.
     */
    @Test
    fun `shows the blocking spinner only until something is cached`() = runTest(dispatcher) {
        val viewModel = viewModel()

        assertTrue(viewModel.uiState.value.isLoading)

        locations.cached.value = earth
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals("Earth (C-137)", viewModel.uiState.value.location?.name)
    }

    /** The offline rule: cached content wins over a failed refresh. */
    @Test
    fun `a failed refresh with a cached location is a banner, not a blank screen`() =
        runTest(dispatcher) {
            locations.cached.value = earth
            locations.refreshResult = Resource.Error(DataError.NoInternet)

            val viewModel = viewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(DataError.NoInternet, state.error)
            assertEquals("Earth (C-137)", state.location?.name)
            assertFalse(state.isBlockingError)
        }

    @Test
    fun `a failed refresh with an empty cache is fatal`() = runTest(dispatcher) {
        locations.refreshResult = Resource.Error(DataError.NoInternet)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isBlockingError)
    }

    /** Spec L4: the resident ids come off the location, so nothing is asked for before it. */
    @Test
    fun `the residents are fetched for the ids the location carries`() = runTest(dispatcher) {
        locations.cached.value = earth

        viewModel()
        advanceUntilIdle()

        assertTrue(characters.refreshedIds.contains(listOf(38, 45)))
    }

    @Test
    fun `an empty location never asks for residents`() = runTest(dispatcher) {
        locations.cached.value = earth.copy(residentIds = emptyList())

        viewModel()
        advanceUntilIdle()

        assertTrue(characters.refreshedIds.all { it.isEmpty() })
    }

    @Test
    fun `renders whatever the resident cache holds`() = runTest(dispatcher) {
        locations.cached.value = earth
        characters.cached.value = listOf(rick)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(rick), viewModel.uiState.value.residents)
    }

    /**
     * The two sections fail independently: the batch call for the residents going down is no
     * reason to take the location off screen.
     */
    @Test
    fun `a failed resident fetch does not disturb the location`() = runTest(dispatcher) {
        locations.cached.value = earth
        characters.refreshResult = Resource.Error(DataError.NoInternet)

        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(DataError.NoInternet, state.residentsError)
        assertEquals("Earth (C-137)", state.location?.name)
        assertFalse(state.isBlockingError)
    }

    @Test
    fun `a failed resident fetch leaves the cached residents on screen`() = runTest(dispatcher) {
        locations.cached.value = earth
        characters.cached.value = listOf(rick)
        characters.refreshResult = Resource.Error(DataError.NoInternet)

        val viewModel = viewModel()
        advanceUntilIdle()

        assertEquals(listOf(rick), viewModel.uiState.value.residents)
    }

    @Test
    fun `pulling to refresh asks for the location again`() = runTest(dispatcher) {
        locations.cached.value = earth
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onEvent(LocationDetailUiEvent.Refreshed)
        advanceUntilIdle()

        assertEquals(listOf(LOCATION_ID, LOCATION_ID), locations.refreshedIds)
    }

    /** Spec L4: a resident opens S3, which is what closes the loop back to characters. */
    @Test
    fun `tapping a resident asks for navigation exactly once`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(LocationDetailUiEvent.CharacterClicked(38))

            assertEquals(LocationDetailUiEffect.NavigateToCharacterDetail(38), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `back is an effect rather than something the screen decides`() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.effects.test {
            viewModel.onEvent(LocationDetailUiEvent.BackClicked)

            assertEquals(LocationDetailUiEffect.NavigateBack, awaitItem())
        }
    }

    private companion object {
        const val LOCATION_ID = 1

        val earth = LocationModel(
            id = LOCATION_ID,
            name = "Earth (C-137)",
            type = "Planet",
            dimension = "Dimension C-137",
            residentIds = listOf(38, 45),
            url = "https://rickandmortyapi.com/api/location/1",
            created = "2017-11-10T12:42:04.162Z"
        )

        val rick = CharacterModel(
            id = 38,
            name = "Beth Smith",
            status = CharacterStatus.Alive,
            species = "Human",
            type = "",
            gender = Gender.Female,
            origin = CharacterOriginModel(name = "Earth (C-137)", id = 1),
            location = CharacterLocationModel(name = "Earth (C-137)", id = 1),
            image = "",
            episodeIds = listOf(1),
            url = "",
            created = ""
        )
    }
}
