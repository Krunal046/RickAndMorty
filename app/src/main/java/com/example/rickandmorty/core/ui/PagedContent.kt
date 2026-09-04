package com.example.rickandmorty.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import com.example.rickandmorty.core.presentation.toMessageRes

/**
 * The one place that decides what a paged screen shows, so no two lists can answer
 * "am I loading?" differently.
 *
 * The rule that matters offline: the screen renders from the local database, and a refresh
 * is a background attempt to update it. So a failed refresh is only fatal when the database
 * had nothing to show - `itemCount > 0` means the cached rows stay on screen and the
 * failure is demoted to a banner. Without that check, the app would blank out every list
 * the moment it lost connectivity, even with a full cache behind it.
 *
 * Pull-to-refresh wraps every case, including the empty and error ones, so a pull is always
 * available as a way out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> PagedContent(
    items: LazyPagingItems<T>,
    @StringRes emptyMessageRes: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val refresh = items.loadState.refresh
    val hasContent = items.itemCount > 0

    PullToRefreshBox(
        // A refresh with nothing cached is the blocking spinner below, not the pull indicator.
        isRefreshing = refresh is LoadState.Loading && hasContent,
        onRefresh = items::refresh,
        modifier = modifier.fillMaxSize()
    ) {
        when {
            refresh is LoadState.Loading && !hasContent -> LoadingState()

            refresh is LoadState.Error && !hasContent -> ErrorState(
                messageRes = refresh.error.toMessageRes(),
                onRetry = items::retry
            )

            // endOfPaginationReached keeps the empty state from flashing before the first
            // page has landed.
            !hasContent && items.loadState.append.endOfPaginationReached ->
                EmptyState(messageRes = emptyMessageRes)

            else -> Column(modifier = Modifier.fillMaxSize()) {
                if (refresh is LoadState.Error) {
                    ErrorBanner(
                        messageRes = refresh.error.toMessageRes(),
                        onRetry = items::retry
                    )
                }
                content()
            }
        }
    }
}

/**
 * The append footer every paged list ends with: a spinner while the next page loads, an
 * inline retry when it fails, nothing otherwise.
 *
 * Call from inside a `LazyColumn`/`LazyVerticalGrid` body - it adds its own item.
 */
fun <T : Any> LazyListScope.pagingAppendFooter(
    items: LazyPagingItems<T>
) {
    when (val append = items.loadState.append) {
        is LoadState.Loading -> item { AppendLoading() }

        is LoadState.Error -> item {
            AppendError(messageRes = append.error.toMessageRes(), onRetry = items::retry)
        }

        is LoadState.NotLoading -> Unit
    }
}
