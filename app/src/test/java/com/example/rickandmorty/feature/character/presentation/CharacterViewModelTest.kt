package com.example.rickandmorty.feature.character.presentation

import androidx.paging.PagingData
import app.cash.turbine.test
import com.example.rickandmorty.feature.character.domain.model.CharacterModel
import com.example.rickandmorty.feature.character.domain.model.CharacterQuery
import com.example.rickandmorty.feature.character.domain.model.CharacterStatus
import com.example.rickandmorty.feature.character.domain.model.Gender
import com.example.rickandmorty.feature.character.domain.repository.CharacterRepository
import com.example.rickandmorty.feature.character.domain.usecase.GetCharacterPagingUseCase
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
class CharacterViewModelTest {

    /** Records the queries the pager was actually rebuilt for. */
    private class RecordingRepository : CharacterRepository {
        val queries = mutableListOf<CharacterQuery>()

        override fun characterPaging(query: CharacterQuery): Flow<PagingData<CharacterModel>> {
            queries += query
            return flowOf(PagingData.empty())
        }

        override fun observeCharacter(id: Int) = error("not used")
        override suspend fun refreshCharacter(id: Int) = error("not used")
        override fun observeCharactersByIds(ids: List<Int>) = error("not used")

        override suspend fun refreshCharacters(ids: List<Int>) = error("not used")
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: RecordingRepository
    private lateinit var viewModel: CharacterViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = RecordingRepository()
        viewModel = CharacterViewModel(GetCharacterPagingUseCase(repository))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `search text lands in state immediately, so typing never feels laggy`() = runTest {
        viewModel.uiState.test {
            assertEquals(CharacterQuery(), awaitItem().query)

            viewModel.onEvent(CharacterUiEvent.SearchChanged("ric"))

            assertEquals("ric", awaitItem().query.name)
        }
    }

    /**
     * The point of the debounce: typing five letters must be one request, not five, or every
     * keystroke would start a page load and a cache entry.
     */
    @Test
    fun `a burst of typing rebuilds the pager once`() = runTest(dispatcher) {
        viewModel.characters.test {
            awaitItem() // the initial unfiltered list

            "rick".forEachIndexed { index, _ ->
                viewModel.onEvent(CharacterUiEvent.SearchChanged("rick".take(index + 1)))
                advanceTimeBy(50)
            }
            advanceTimeBy(400)
            awaitItem()

            assertEquals(
                listOf(CharacterQuery(), CharacterQuery(name = "rick")),
                repository.queries
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Clearing the box must not sit behind the debounce - the plain list is already cached. */
    @Test
    fun `clearing the search applies without waiting`() = runTest(dispatcher) {
        viewModel.characters.test {
            awaitItem()

            viewModel.onEvent(CharacterUiEvent.SearchChanged("rick"))
            advanceTimeBy(400)
            awaitItem()

            viewModel.onEvent(CharacterUiEvent.SearchChanged(""))
            advanceTimeBy(1)
            awaitItem()

            assertEquals(CharacterQuery(), repository.queries.last())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tapping the selected status chip again clears it`() = runTest {
        viewModel.onEvent(CharacterUiEvent.StatusToggled(CharacterStatus.Alive))
        assertEquals(CharacterStatus.Alive, viewModel.uiState.value.query.status)

        viewModel.onEvent(CharacterUiEvent.StatusToggled(CharacterStatus.Alive))
        assertEquals(null, viewModel.uiState.value.query.status)
    }

    @Test
    fun `tapping a different chip in the group replaces the selection`() = runTest {
        viewModel.onEvent(CharacterUiEvent.StatusToggled(CharacterStatus.Alive))
        viewModel.onEvent(CharacterUiEvent.StatusToggled(CharacterStatus.Dead))

        assertEquals(CharacterStatus.Dead, viewModel.uiState.value.query.status)
    }

    @Test
    fun `filters combine rather than replace one another`() = runTest {
        viewModel.onEvent(CharacterUiEvent.StatusToggled(CharacterStatus.Alive))
        viewModel.onEvent(CharacterUiEvent.GenderToggled(Gender.Female))
        viewModel.onEvent(CharacterUiEvent.SpeciesToggled("Human"))

        val query = viewModel.uiState.value.query
        assertEquals(CharacterStatus.Alive, query.status)
        assertEquals(Gender.Female, query.gender)
        assertEquals("Human", query.species)
        assertEquals("character:status=alive&species=human&gender=female", query.cacheKey)
    }

    /** "Clear filters" is what the button says; the search text is not a filter. */
    @Test
    fun `clearing filters keeps the search text`() = runTest {
        viewModel.onEvent(CharacterUiEvent.SearchChanged("rick"))
        viewModel.onEvent(CharacterUiEvent.StatusToggled(CharacterStatus.Alive))

        viewModel.onEvent(CharacterUiEvent.FiltersCleared)

        assertEquals("rick", viewModel.uiState.value.query.name)
        assertEquals(null, viewModel.uiState.value.query.status)
    }

    @Test
    fun `tapping a character asks for navigation exactly once`() = runTest {
        viewModel.effects.test {
            viewModel.onEvent(CharacterUiEvent.CharacterClicked(42))

            assertEquals(CharacterUiEffect.NavigateToCharacterDetail(42), awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `the filter panel starts collapsed and toggles`() = runTest {
        assertFalse(viewModel.uiState.value.filtersExpanded)

        viewModel.onEvent(CharacterUiEvent.FiltersToggled)

        assertTrue(viewModel.uiState.value.filtersExpanded)
    }
}
