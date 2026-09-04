package com.example.rickandmorty.feature.episode.presentation

import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.testing.asSnapshot
import app.cash.turbine.test
import com.example.rickandmorty.feature.episode.domain.model.EpisodeModel
import com.example.rickandmorty.feature.episode.domain.model.EpisodeQuery
import com.example.rickandmorty.feature.episode.domain.repository.EpisodeRepository
import com.example.rickandmorty.feature.episode.domain.usecase.GetEpisodePagingUseCase
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EpisodeViewModelTest {

    /** Records the queries the pager was rebuilt for, and serves a fixed page of episodes. */
    private class RecordingRepository : EpisodeRepository {
        val queries = mutableListOf<EpisodeQuery>()
        var episodes: List<EpisodeModel> = emptyList()

        override fun episodePaging(query: EpisodeQuery): Flow<PagingData<EpisodeModel>> {
            queries += query
            return flowOf(
                PagingData.from(
                    data = episodes,
                    // Terminal separators are only emitted at an end the list has actually
                    // reached, so a heading above the first episode needs this to be said.
                    sourceLoadStates = LoadStates(
                        refresh = LoadState.NotLoading(endOfPaginationReached = false),
                        prepend = LoadState.NotLoading(endOfPaginationReached = true),
                        append = LoadState.NotLoading(endOfPaginationReached = true)
                    )
                )
            )
        }

        override fun observeEpisodes(ids: List<Int>) = error("not used")
        override suspend fun refreshEpisodes(ids: List<Int>) = error("not used")
        override fun observeEpisode(id: Int) = error("not used")
        override suspend fun refreshEpisode(id: Int) = error("not used")
    }

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: RecordingRepository
    private lateinit var viewModel: EpisodeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = RecordingRepository()
        viewModel = EpisodeViewModel(GetEpisodePagingUseCase(repository))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun episode(id: Int, code: String, name: String = "Episode $id") = EpisodeModel(
        id = id,
        name = name,
        airDate = "December 2, 2013",
        code = code,
        characterIds = emptyList(),
        url = "",
        created = ""
    )

    @Test
    fun `search text lands in state immediately, so typing never feels laggy`() = runTest {
        viewModel.uiState.test {
            assertEquals(EpisodeQuery(), awaitItem().query)

            viewModel.onEvent(EpisodeUiEvent.SearchChanged("pil"))

            assertEquals("pil", awaitItem().query.search)
        }
    }

    /**
     * The point of the debounce: typing five letters must be one request, not five, or every
     * keystroke would start a page load and a cache entry.
     */
    @Test
    fun `a burst of typing rebuilds the pager once`() = runTest(dispatcher) {
        viewModel.episodes.test {
            awaitItem() // the initial unfiltered list

            "pilot".forEachIndexed { index, _ ->
                viewModel.onEvent(EpisodeUiEvent.SearchChanged("pilot".take(index + 1)))
                advanceTimeBy(50)
            }
            advanceTimeBy(400)
            awaitItem()

            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(
            listOf(EpisodeQuery(), EpisodeQuery("pilot")),
            repository.queries
        )
    }

    @Test
    fun `clearing the box applies at once rather than waiting out the debounce`() =
        runTest(dispatcher) {
            viewModel.episodes.test {
                awaitItem()

                viewModel.onEvent(EpisodeUiEvent.SearchChanged("pilot"))
                advanceTimeBy(400)
                awaitItem()

                viewModel.onEvent(EpisodeUiEvent.SearchChanged(""))
                awaitItem()

                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(
                listOf(EpisodeQuery(), EpisodeQuery("pilot"), EpisodeQuery()),
                repository.queries
            )
        }

    @Test
    fun `tapping an episode asks for navigation exactly once`() = runTest(dispatcher) {
        viewModel.effects.test {
            viewModel.onEvent(EpisodeUiEvent.EpisodeClicked(7))

            assertEquals(EpisodeUiEffect.NavigateToEpisodeDetail(7), awaitItem())
            expectNoEvents()
        }
    }

    /** Spec E1: the list is grouped by season, and the first season needs a heading too. */
    @Test
    fun `a heading is inserted above the first episode of every season`() = runTest(dispatcher) {
        repository.episodes = listOf(
            episode(1, "S01E01"),
            episode(2, "S01E02"),
            episode(12, "S02E01"),
            episode(23, "S03E01")
        )

        val items = viewModel.episodes.asSnapshot()

        assertEquals(
            listOf(
                EpisodeListItem.Header(1),
                EpisodeListItem.Item(episode(1, "S01E01")),
                EpisodeListItem.Item(episode(2, "S01E02")),
                EpisodeListItem.Header(2),
                EpisodeListItem.Item(episode(12, "S02E01")),
                EpisodeListItem.Header(3),
                EpisodeListItem.Item(episode(23, "S03E01"))
            ),
            items
        )
    }

    /** A heading with nothing under it would be a lie, so the list never ends on one. */
    @Test
    fun `no trailing heading is added at the end of the list`() = runTest(dispatcher) {
        repository.episodes = listOf(episode(1, "S01E01"))

        val items = viewModel.episodes.asSnapshot()

        assertEquals(
            listOf(EpisodeListItem.Header(1), EpisodeListItem.Item(episode(1, "S01E01"))),
            items
        )
    }

    /** An unreadable code groups on its own rather than being filed under season 1. */
    @Test
    fun `episodes with an unreadable code get their own heading`() = runTest(dispatcher) {
        repository.episodes = listOf(episode(1, "S01E01"), episode(2, "unknown"))

        val items = viewModel.episodes.asSnapshot()

        assertEquals(
            listOf(
                EpisodeListItem.Header(1),
                EpisodeListItem.Item(episode(1, "S01E01")),
                EpisodeListItem.Header(null),
                EpisodeListItem.Item(episode(2, "unknown"))
            ),
            items
        )
    }

    @Test
    fun `an empty list gets no headings at all`() = runTest(dispatcher) {
        repository.episodes = emptyList()

        assertEquals(emptyList<EpisodeListItem>(), viewModel.episodes.asSnapshot())
    }
}
