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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter.consumer.ui.LocalAppContainer
import com.smartmeter.consumer.ui.components.ErrorBanner
import com.smartmeter.consumer.ui.components.ScreenScaffold
import com.smartmeter.consumer.ui.components.supportingError
import com.smartmeter.consumer.ui.theme.SuccessColor

@Composable
fun ChangePasswordScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ChangePasswordViewModel = viewModel { ChangePasswordViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var visible by rememberSaveable { mutableStateOf(false) }

    if (state.saved) {
        AlertDialog(
            onDismissRequest = onBack,
            icon = { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = SuccessColor) },
            title = { Text("Password changed") },
            text = { Text("Use your new password the next time you log in.") },
            confirmButton = { TextButton(onClick = onBack) { Text("OK") } },
        )
    }

    ScreenScaffold(title = "Change password", onBack = onBack, hasBottomBar = false) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val transformation = if (visible) VisualTransformation.None else PasswordVisualTransformation()
            val toggle: @Composable () -> Unit = {
                IconButton(onClick = { visible = !visible }) {
                    Icon(
                        if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (visible) "Hide passwords" else "Show passwords",
                    )
                }
            }

            OutlinedTextField(
                value = state.current,
                onValueChange = viewModel::onCurrentChange,
                label = { Text("Current password") },
                singleLine = true,
                visualTransformation = transformation,
                trailingIcon = toggle,
                isError = "current_password" in state.fieldErrors,
                supportingText = supportingError(state.fieldErrors["current_password"]),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.new,
                onValueChange = viewModel::onNewChange,
                label = { Text("New password") },
                singleLine = true,
                visualTransformation = transformation,
                isError = "new_password" in state.fieldErrors,
                supportingText = supportingError(state.fieldErrors["new_password"] ?: "At least 6 characters"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.confirm,
                onValueChange = viewModel::onConfirmChange,
                label = { Text("Confirm new password") },
                singleLine = true,
                visualTransformation = transformation,
                isError = "confirm" in state.fieldErrors,
                supportingText = supportingError(state.fieldErrors["confirm"]),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
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
                    Text("Change password", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
