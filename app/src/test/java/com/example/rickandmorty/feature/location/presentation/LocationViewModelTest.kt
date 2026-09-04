package com.example.rickandmorty.feature.location.presentation

import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.rickandmorty.feature.location.domain.model.LocationModel
import com.example.rickandmorty.feature.location.domain.model.LocationQuery
import com.example.rickandmorty.feature.location.domain.repository.LocationRepository
import com.example.rickandmorty.feature.location.domain.usecase.GetLocationPagingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class LocationViewModelTest {

    /** Records the queries the pager was actually rebuilt for. */
    private class RecordingRepository : LocationRepository {
        val queries = mutableListOf<LocationQuery>()

        override fun locationPaging(query: LocationQuery): Flow<PagingData<LocationModel>> {
            queries += query
            return flowOf(PagingData.empty())
        }

        override fun observeLocation(id: Int) = error("not used")
        override suspend fun refreshLocation(id: Int) = error("not used")
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: RecordingRepository
    private lateinit var viewModel: LocationViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = RecordingRepository()
        viewModel = LocationViewModel(GetLocationPagingUseCase(repository))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `search text lands in state immediately, so typing never feels laggy`() = runTest {
        viewModel.uiState.test {
            assertEquals(LocationQuery(), awaitItem().query)

            viewModel.onEvent(LocationUiEvent.SearchChanged("ear"))

            assertEquals("ear", awaitItem().query.name)
        }
    }

    /** The point of the debounce: typing must be one request, not one per keystroke. */
    @Test
    fun `a burst of typing rebuilds the pager once`() = runTest(dispatcher) {
        viewModel.locations.test {
            awaitItem() // the initial unfiltered list

            "earth".forEachIndexed { index, _ ->
                viewModel.onEvent(LocationUiEvent.SearchChanged("earth".take(index + 1)))
                advanceTimeBy(50)
            }
            advanceTimeBy(400)
            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(
            listOf(LocationQuery(), LocationQuery(name = "earth")),
            repository.queries
        )
    }

    /** A chip is a single tap, so there is nothing to wait out. */
    @Test
    fun `toggling a filter applies at once rather than waiting out the debounce`() =
        runTest(dispatcher) {
            viewModel.locations.test {
                awaitItem()

                viewModel.onEvent(LocationUiEvent.TypeToggled("Planet"))
                awaitItem()

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                listOf(LocationQuery(), LocationQuery(type = "Planet")),
                repository.queries
            )
        }

    @Test
    fun `tapping the selected chip again clears that filter`() = runTest(dispatcher) {
        viewModel.onEvent(LocationUiEvent.TypeToggled("Planet"))
        assertEquals("Planet", viewModel.uiState.value.query.type)

        viewModel.onEvent(LocationUiEvent.TypeToggled("Planet"))
        assertEquals("", viewModel.uiState.value.query.type)
    }

    @Test
    fun `tapping a different chip replaces the selection`() = runTest(dispatcher) {
        viewModel.onEvent(LocationUiEvent.DimensionToggled("unknown"))
        viewModel.onEvent(LocationUiEvent.DimensionToggled("Dimension C-137"))

        assertEquals("Dimension C-137", viewModel.uiState.value.query.dimension)
    }

    @Test
    fun `filters combine rather than replacing one another`() = runTest(dispatcher) {
        viewModel.onEvent(LocationUiEvent.SearchChanged("earth"))
        viewModel.onEvent(LocationUiEvent.TypeToggled("Planet"))
        viewModel.onEvent(LocationUiEvent.DimensionToggled("unknown"))

        assertEquals(
            LocationQuery(name = "earth", type = "Planet", dimension = "unknown"),
            viewModel.uiState.value.query
        )
    }

    /** "Clear filters" is what the button says, so the search text stays. */
    @Test
    fun `clearing the filters keeps the search text`() = runTest(dispatcher) {
        viewModel.onEvent(LocationUiEvent.SearchChanged("earth"))
        viewModel.onEvent(LocationUiEvent.TypeToggled("Planet"))

        viewModel.onEvent(LocationUiEvent.FiltersCleared)

        assertEquals(LocationQuery(name = "earth"), viewModel.uiState.value.query)
    }

    @Test
    fun `the filter panel starts collapsed and toggles`() = runTest(dispatcher) {
        assertFalse(viewModel.uiState.value.filtersExpanded)

        viewModel.onEvent(LocationUiEvent.FiltersToggled)

        assertTrue(viewModel.uiState.value.filtersExpanded)
    }

    @Test
    fun `tapping a location asks for navigation exactly once`() = runTest(dispatcher) {
        viewModel.effects.test {
            viewModel.onEvent(LocationUiEvent.LocationClicked(3))

            assertEquals(LocationUiEffect.NavigateToLocationDetail(3), awaitItem())
            expectNoEvents()
        }
    }
}
