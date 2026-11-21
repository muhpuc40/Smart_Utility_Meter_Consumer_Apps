package com.smartmeter.consumer.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter.consumer.data.ApiResult
import com.smartmeter.consumer.data.MeterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel(private val repository: MeterRepository) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onIdentifierChange(value: String) = _state.update { it.copy(identifier = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun login() {
        val current = _state.value
        if (current.loading) return

        if (current.identifier.isBlank() || current.password.isEmpty()) {
            _state.update { it.copy(error = "Enter your phone number or meter ID and password") }
            return
        }

        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            when (val result = repository.login(current.identifier, current.password)) {
                // On success the saved session switches the app to the main screen
                is ApiResult.Success -> _state.update { it.copy(loading = false, password = "") }
                is ApiResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
