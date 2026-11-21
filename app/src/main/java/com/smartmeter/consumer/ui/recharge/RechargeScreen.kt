package com.smartmeter.consumer.ui.recharge

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter.consumer.data.remote.Recharge
import com.smartmeter.consumer.ui.LocalAppContainer
import com.smartmeter.consumer.ui.components.ErrorBanner
import com.smartmeter.consumer.ui.components.ScreenScaffold
import com.smartmeter.consumer.ui.components.SectionCard
import com.smartmeter.consumer.ui.components.StatusBadge
import com.smartmeter.consumer.ui.components.supportingError
import com.smartmeter.consumer.ui.theme.SuccessColor
import com.smartmeter.consumer.ui.theme.WarningColor
import com.smartmeter.consumer.ui.util.RECHARGE_METHODS
import com.smartmeter.consumer.ui.util.methodLabel
import com.smartmeter.consumer.ui.util.money
import com.smartmeter.consumer.ui.util.relayColor
import com.smartmeter.consumer.ui.util.stateLabel

private val QUICK_AMOUNTS = listOf(100, 200, 500, 1000, 2000)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RechargeScreen(onOpenHistory: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: RechargeViewModel = viewModel { RechargeViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    state.submitted?.let { recharge ->
        RechargeResultDialog(
            recharge = recharge,
            onDismiss = viewModel::dismissResult,
            onOpenHistory = {
                viewModel.dismissResult()
                onOpenHistory()
            },
        )
    }

    ScreenScaffold(
        title = "Recharge",
        actions = {
            TextButton(onClick = onOpenHistory) {
                Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("History")
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ---------- Current balance ----------
            SectionCard(title = null) {
                Text(
                    "Current balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                val balance = state.balance

                when {
                    balance != null -> {
                        Text(money(balance.balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        StatusBadge(stateLabel(balance.state), relayColor(balance.relay))
                    }

                    state.balanceLoading -> CircularProgressIndicator(Modifier.padding(top = 8.dp).size(24.dp))

                    else -> TextButton(onClick = viewModel::loadBalance) {
                        Text(state.balanceError ?: "Could not load balance. Tap to retry")
                    }
                }
            }

            // ---------- How it works ----------
            SectionCard(title = "How to recharge") {
                Text(
                    "1. Send money to the meter office using bKash, Nagad, Rocket or another method.\n" +
                        "2. Enter the amount and the transaction ID from your payment SMS below.\n" +
                        "3. Your balance is added after the office verifies the payment.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ---------- Form ----------
            SectionCard(title = "Recharge details") {

                OutlinedTextField(
                    value = state.amount,
                    onValueChange = viewModel::onAmountChange,
                    label = { Text("Amount") },
                    prefix = { Text("৳ ") },
                    singleLine = true,
                    isError = "amount" in state.fieldErrors,
                    supportingText = supportingError(state.fieldErrors["amount"]),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QUICK_AMOUNTS.forEach { amount ->
                        SuggestionChip(
                            onClick = { viewModel.onQuickAmount(amount) },
                            label = { Text("৳$amount") },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text("Payment method", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RECHARGE_METHODS.forEach { method ->
                        FilterChip(
                            selected = state.method == method,
                            onClick = { viewModel.onMethodChange(method) },
                            label = { Text(methodLabel(method)) },
                        )
                    }
                }

                state.fieldErrors["method"]?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.transactionRef,
                    onValueChange = viewModel::onTransactionRefChange,
                    label = { Text("Transaction ID") },
                    placeholder = { Text("e.g. 9KX7A2B3C4") },
                    singleLine = true,
                    isError = "transaction_ref" in state.fieldErrors,
                    supportingText = supportingError(state.fieldErrors["transaction_ref"]),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Note (optional)") },
                    placeholder = { Text("e.g. sent from 01XXXXXXXXX") },
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                )

                state.error?.let {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(it)
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.submit()
                    },
                    enabled = !state.submitting,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    else {
                        val amount = state.amount.toDoubleOrNull()
                        Text(
                            if (amount != null && amount > 0) "Submit ${money(amount)} recharge" else "Submit recharge",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun RechargeResultDialog(recharge: Recharge, onDismiss: () -> Unit, onOpenHistory: () -> Unit) {
    val approved = recharge.status == "approved"

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                if (approved) Icons.Outlined.CheckCircle else Icons.Outlined.Schedule,
                contentDescription = null,
                tint = if (approved) SuccessColor else WarningColor,
            )
        },
        title = { Text(if (approved) "Recharge successful" else "Recharge submitted") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(money(recharge.amount), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("${methodLabel(recharge.method)} · ${recharge.transactionRef.orEmpty()}")
                Spacer(Modifier.height(12.dp))
                Text(
                    if (approved) {
                        "Your new balance is ${money(recharge.balanceAfter ?: 0.0)}."
                    }
                    else {
                        "Your request is pending. The balance will be added after the office verifies your transaction ID."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
        dismissButton = { TextButton(onClick = onOpenHistory) { Text("View history") } },
    )
}
