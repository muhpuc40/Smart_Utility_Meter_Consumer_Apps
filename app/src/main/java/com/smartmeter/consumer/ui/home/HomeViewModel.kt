package com.smartmeter.consumer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter.consumer.data.ApiResult
import com.smartmeter.consumer.data.MeterRepository
import com.smartmeter.consumer.data.remote.Profile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val profile: Profile? = null,
    val error: String? = null,
)

class HomeViewModel(private val repository: MeterRepository) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        load()
    }

    // pullToRefresh: show the refresh spinner; silent: background auto-refresh
    fun load(pullToRefresh: Boolean = false, silent: Boolean = false) {
        if (job?.isActive == true && silent) return
        job?.cancel()

        if (!silent) {
            _state.update {
                it.copy(loading = it.profile == null, refreshing = pullToRefresh, error = null)
            }
        }

        job = viewModelScope.launch {
            when (val result = repository.profile()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, refreshing = false, profile = result.data, error = null)
                }

                is ApiResult.Error -> _state.update {
                    it.copy(loading = false, refreshing = false, error = result.message)
                }
            }
        }
    }
}
