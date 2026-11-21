package com.smartmeter.consumer.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter.consumer.data.MeterRepository
import com.smartmeter.consumer.data.remote.Recharge
import com.smartmeter.consumer.data.remote.Transaction
import com.smartmeter.consumer.ui.util.PagedLoader
import com.smartmeter.consumer.ui.util.PagedState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// null = all statuses
val RECHARGE_FILTERS = listOf<String?>(null, "pending", "approved", "rejected")

class HistoryViewModel(repository: MeterRepository) : ViewModel() {

    private val _rechargeFilter = MutableStateFlow<String?>(null)
    val rechargeFilter: StateFlow<String?> = _rechargeFilter.asStateFlow()

    private val rechargeLoader = PagedLoader(viewModelScope) { page ->
        repository.recharges(page, _rechargeFilter.value)
    }

    private val transactionLoader = PagedLoader(viewModelScope) { page ->
        repository.transactions(page)
    }

    val recharges: StateFlow<PagedState<Recharge>> = rechargeLoader.state
    val transactions: StateFlow<PagedState<Transaction>> = transactionLoader.state

    init {
        rechargeLoader.refresh()
        transactionLoader.refresh()
    }

    fun setRechargeFilter(status: String?) {
        if (status == _rechargeFilter.value) return
        _rechargeFilter.value = status
        rechargeLoader.refresh()
    }

    fun refreshRecharges(pullToRefresh: Boolean = false) = rechargeLoader.refresh(pullToRefresh)

    fun loadMoreRecharges() = rechargeLoader.loadMore()

    fun refreshTransactions(pullToRefresh: Boolean = false) = transactionLoader.refresh(pullToRefresh)

    fun loadMoreTransactions() = transactionLoader.loadMore()
}
