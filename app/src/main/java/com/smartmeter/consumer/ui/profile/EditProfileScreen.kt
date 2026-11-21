package com.smartmeter.consumer.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter.consumer.ui.LocalAppContainer
import com.smartmeter.consumer.ui.components.ErrorBanner
import com.smartmeter.consumer.ui.components.ErrorState
import com.smartmeter.consumer.ui.components.LoadingState
import com.smartmeter.consumer.ui.components.ScreenScaffold
import com.smartmeter.consumer.ui.components.supportingError

@Composable
fun EditProfileScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: EditProfileViewModel = viewModel { EditProfileViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    ScreenScaffold(title = "Edit profile", onBack = onBack, hasBottomBar = false) { padding ->
        when {
            state.loading -> LoadingState(Modifier.padding(padding))

            state.name.isEmpty() && state.error != null -> ErrorState(
                message = state.error ?: "",
                onRetry = viewModel::load,
                modifier = Modifier.padding(padding),
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Full name") },
                    singleLine = true,
                    isError = "name" in state.fieldErrors,
                    supportingText = supportingError(state.fieldErrors["name"]),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.email,
                    onValueChange = viewModel::onEmailChange,
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    isError = "email" in state.fieldErrors,
                    supportingText = supportingError(state.fieldErrors["email"]),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.address,
                    onValueChange = viewModel::onAddressChange,
                    label = { Text("Address (optional)") },
                    maxLines = 3,
                    isError = "address" in state.fieldErrors,
                    supportingText = supportingError(state.fieldErrors["address"]),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = state.phone,
                    onValueChange = {},
                    label = { Text("Phone") },
                    enabled = false,
                    supportingText = { Text("Contact the office to change your phone number") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.error?.let { ErrorBanner(it) }

                Button(
                    onClick = viewModel::save,
                    enabled = !state.saving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    else {
                        Text("Save changes", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
