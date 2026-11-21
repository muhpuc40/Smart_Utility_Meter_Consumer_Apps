package com.smartmeter.consumer.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter.consumer.data.ApiResult
import com.smartmeter.consumer.data.MeterRepository
import com.smartmeter.consumer.data.remote.Reading
import com.smartmeter.consumer.data.remote.UsageSummary
import com.smartmeter.consumer.ui.util.PagedLoader
import com.smartmeter.consumer.ui.util.PagedState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

enum class UsageRange(val label: String, val days: Long) {
    WEEK("7 days", 7),
    MONTH("30 days", 30),
    QUARTER("90 days", 90),
}

enum class UsageMetric(val label: String) {
    COST("Cost"),
    ENERGY("Electricity"),
    WATER("Water"),
}

data class UsageUiState(
    val range: UsageRange = UsageRange.MONTH,
    val metric: UsageMetric = UsageMetric.COST,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val summary: UsageSummary? = null,
    val error: String? = null,
)

class UsageViewModel(private val repository: MeterRepository) : ViewModel() {

    private val _state = MutableStateFlow(UsageUiState())
    val state: StateFlow<UsageUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load()
    }

    fun selectRange(range: UsageRange) {
        if (range == _state.value.range) return
        _state.update { it.copy(range = range) }
        load()
    }

    fun selectMetric(metric: UsageMetric) = _state.update { it.copy(metric = metric) }

    fun load(pullToRefresh: Boolean = false) {
        job?.cancel()
        _state.update { it.copy(loading = !pullToRefresh, refreshing = pullToRefresh, error = null) }

        val to = Instant.now()
        val from = to.minus(_state.value.range.days, ChronoUnit.DAYS)

        job = viewModelScope.launch {
            when (val result = repository.usageSummary(from, to)) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, refreshing = false, summary = result.data, error = null)
                }

                is ApiResult.Error -> _state.update {
                    it.copy(loading = false, refreshing = false, error = result.message)
                }
            }
        }
    }
}

class ReadingsViewModel(repository: MeterRepository) : ViewModel() {

    private val loader = PagedLoader(viewModelScope) { page -> repository.readings(page) }
    val state: StateFlow<PagedState<Reading>> = loader.state

    init {
        loader.refresh()
    }

    fun refresh(pullToRefresh: Boolean = false) = loader.refresh(pullToRefresh)

    fun loadMore() = loader.loadMore()
}
