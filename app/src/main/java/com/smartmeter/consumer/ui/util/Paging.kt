package com.smartmeter.consumer.ui.util

import com.smartmeter.consumer.data.ApiResult
import com.smartmeter.consumer.data.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PagedState<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val totalPages: Int = 0,
    val total: Int = 0,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loaded: Boolean = false,
    val error: String? = null,
) {
    val canLoadMore: Boolean get() = page < totalPages
}

// Loads a paginated API list page by page ("infinite scroll")
class PagedLoader<T>(
    private val scope: CoroutineScope,
    private val fetch: suspend (page: Int) -> ApiResult<Page<T>>,
) {
    private val _state = MutableStateFlow(PagedState<T>())
    val state: StateFlow<PagedState<T>> = _state.asStateFlow()

    private var job: Job? = null

    fun refresh(pullToRefresh: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(loading = !pullToRefresh, refreshing = pullToRefresh, error = null) }
        job = scope.launch { load(page = 1, reset = true) }
    }

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.refreshing || !current.canLoadMore) return

        _state.update { it.copy(loading = true, error = null) }
        job = scope.launch { load(page = current.page + 1, reset = false) }
    }

    private suspend fun load(page: Int, reset: Boolean) {
        when (val result = fetch(page)) {
            is ApiResult.Success -> _state.update { state ->
                state.copy(
                    items = if (reset) result.data.items else state.items + result.data.items,
                    page = result.data.meta.page,
                    totalPages = result.data.meta.totalPages,
                    total = result.data.meta.total,
                    loading = false,
                    refreshing = false,
                    loaded = true,
                    error = null,
                )
            }

            is ApiResult.Error -> _state.update { state ->
                state.copy(loading = false, refreshing = false, loaded = true, error = result.message)
            }
        }
    }
}
