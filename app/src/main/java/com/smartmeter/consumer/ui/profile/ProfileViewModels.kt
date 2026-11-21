package com.smartmeter.consumer.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartmeter.consumer.data.ApiResult
import com.smartmeter.consumer.data.MeterRepository
import com.smartmeter.consumer.data.remote.Profile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ------------------------------------------------------------
// Profile
// ------------------------------------------------------------

data class ProfileUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val profile: Profile? = null,
    val error: String? = null,
)

class ProfileViewModel(private val repository: MeterRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun load(pullToRefresh: Boolean = false) {
        _state.update { it.copy(loading = it.profile == null, refreshing = pullToRefresh, error = null) }

        viewModelScope.launch {
            when (val result = repository.profile()) {
                is ApiResult.Success -> _state.update {
                    it.copy(loading = false, refreshing = false, profile = result.data)
                }

                is ApiResult.Error -> _state.update {
                    it.copy(loading = false, refreshing = false, error = result.message)
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch { repository.logout() }
    }
}

// ------------------------------------------------------------
// Edit profile
// ------------------------------------------------------------

data class EditProfileUiState(
    val loading: Boolean = true,
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val phone: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
)

class EditProfileViewModel(private val repository: MeterRepository) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }

        viewModelScope.launch {
            when (val result = repository.profile()) {
                is ApiResult.Success -> _state.update {
                    it.copy(
                        loading = false,
                        name = result.data.name,
                        email = result.data.email.orEmpty(),
                        address = result.data.address.orEmpty(),
                        phone = result.data.phone,
                    )
                }

                is ApiResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, fieldErrors = it.fieldErrors - "name") }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, fieldErrors = it.fieldErrors - "email") }

    fun onAddressChange(value: String) = _state.update { it.copy(address = value, fieldErrors = it.fieldErrors - "address") }

    fun save() {
        val current = _state.value
        if (current.saving) return

        if (current.name.trim().length < 2) {
            _state.update { it.copy(fieldErrors = mapOf("name" to "Name must be at least 2 characters")) }
            return
        }

        _state.update { it.copy(saving = true, error = null, fieldErrors = emptyMap()) }

        viewModelScope.launch {
            when (val result = repository.updateProfile(current.name, current.email, current.address)) {
                is ApiResult.Success -> _state.update { it.copy(saving = false, saved = true) }
                is ApiResult.Error -> _state.update {
                    it.copy(saving = false, error = result.message, fieldErrors = result.fieldErrors)
                }
            }
        }
    }
}

// ------------------------------------------------------------
// Change password
// ------------------------------------------------------------

data class ChangePasswordUiState(
    val current: String = "",
    val new: String = "",
    val confirm: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false,
    val fieldErrors: Map<String, String> = emptyMap(),
    val error: String? = null,
)

class ChangePasswordViewModel(private val repository: MeterRepository) : ViewModel() {

    private val _state = MutableStateFlow(ChangePasswordUiState())
    val state: StateFlow<ChangePasswordUiState> = _state.asStateFlow()

    fun onCurrentChange(value: String) = _state.update { it.copy(current = value, fieldErrors = it.fieldErrors - "current_password", error = null) }

    fun onNewChange(value: String) = _state.update { it.copy(new = value, fieldErrors = it.fieldErrors - "new_password", error = null) }

    fun onConfirmChange(value: String) = _state.update { it.copy(confirm = value, fieldErrors = it.fieldErrors - "confirm", error = null) }

    fun save() {
        val current = _state.value
        if (current.saving) return

        val errors = buildMap {
            if (current.current.isEmpty()) put("current_password", "Enter your current password")
            if (current.new.length < 6) put("new_password", "Password must be at least 6 characters")
            if (current.confirm != current.new) put("confirm", "Passwords do not match")
        }

        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors) }
            return
        }

        _state.update { it.copy(saving = true, error = null, fieldErrors = emptyMap()) }

        viewModelScope.launch {
            when (val result = repository.changePassword(current.current, current.new)) {
                is ApiResult.Success -> _state.update { it.copy(saving = false, saved = true) }
                is ApiResult.Error -> _state.update {
                    it.copy(saving = false, error = result.message, fieldErrors = result.fieldErrors)
                }
            }
        }
    }
}
