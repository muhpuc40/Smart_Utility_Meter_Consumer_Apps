package com.smartmeter.consumer.ui.recharge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter.consumer.data.ApiResult
import com.smartmeter.consumer.data.MeterRepository
import com.smartmeter.consumer.data.remote.BalanceInfo
import com.smartmeter.consumer.data.remote.Recharge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RechargeUiState(
    val balance: BalanceInfo? = null,
    val balanceLoading: Boolean = true,
    val balanceError: String? = null,

    val amount: String = "",
    val method: String = "bkash",
    val transactionRef: String = "",
    val note: String = "",

    val submitting: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
    val submitted: Recharge? = null,
)

class RechargeViewModel(private val repository: MeterRepository) : ViewModel() {

    private val _state = MutableStateFlow(RechargeUiState())
    val state: StateFlow<RechargeUiState> = _state.asStateFlow()

    init {
        loadBalance()
    }

    fun loadBalance() {
        _state.update { it.copy(balanceLoading = true, balanceError = null) }

        viewModelScope.launch {
            when (val result = repository.balance()) {
                is ApiResult.Success -> _state.update { it.copy(balanceLoading = false, balance = result.data) }
                is ApiResult.Error -> _state.update { it.copy(balanceLoading = false, balanceError = result.message) }
            }
        }
    }

    // Only digits and one decimal point, at most 2 decimals
    fun onAmountChange(value: String) {
        if (value.isEmpty() || Regex("^\\d{0,7}(\\.\\d{0,2})?$").matches(value)) {
            _state.update { it.copy(amount = value, fieldErrors = it.fieldErrors - "amount", error = null) }
        }
    }

    fun onQuickAmount(amount: Int) =
        _state.update { it.copy(amount = amount.toString(), fieldErrors = it.fieldErrors - "amount", error = null) }

    fun onMethodChange(method: String) =
        _state.update { it.copy(method = method, fieldErrors = it.fieldErrors - "method", error = null) }

    fun onTransactionRefChange(value: String) =
        _state.update { it.copy(transactionRef = value.trim(), fieldErrors = it.fieldErrors - "transaction_ref", error = null) }

    fun onNoteChange(value: String) = _state.update { it.copy(note = value.take(300)) }

    fun dismissResult() = _state.update { it.copy(submitted = null) }

    fun submit() {
        val current = _state.value
        if (current.submitting) return

        val amount = current.amount.toDoubleOrNull()
        val errors = buildMap {
            if (amount == null || amount <= 0.0) put("amount", "Enter an amount greater than 0")
            if (current.transactionRef.length < 4) put("transaction_ref", "Enter the transaction ID from your payment SMS")
        }

        if (errors.isNotEmpty() || amount == null) {
            _state.update { it.copy(fieldErrors = errors) }
            return
        }

        _state.update { it.copy(submitting = true, fieldErrors = emptyMap(), error = null) }

        viewModelScope.launch {
            val result = repository.createRecharge(amount, current.method, current.transactionRef, current.note)

            when (result) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(
                            submitting = false,
                            submitted = result.data,
                            amount = "",
                            transactionRef = "",
                            note = "",
                        )
                    }
                    loadBalance()
                }

                is ApiResult.Error -> _state.update {
                    it.copy(submitting = false, error = result.message, fieldErrors = result.fieldErrors)
                }
            }
        }
    }
}
