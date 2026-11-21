package com.smartmeter.consumer.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter.consumer.data.remote.Reading
import com.smartmeter.consumer.ui.LocalAppContainer
import com.smartmeter.consumer.ui.components.EmptyState
import com.smartmeter.consumer.ui.components.ErrorState
import com.smartmeter.consumer.ui.components.ListFooter
import com.smartmeter.consumer.ui.components.LoadingState
import com.smartmeter.consumer.ui.components.ScreenScaffold
import com.smartmeter.consumer.ui.components.SectionCard
import com.smartmeter.consumer.ui.util.formatDateTime
import com.smartmeter.consumer.ui.util.money
import com.smartmeter.consumer.ui.util.number

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingsScreen(onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: ReadingsViewModel = viewModel { ReadingsViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Meter readings", onBack = onBack, hasBottomBar = false) { padding ->
        when {
            !state.loaded && state.items.isEmpty() -> LoadingState(Modifier.padding(padding))

            state.items.isEmpty() && state.error != null -> ErrorState(
                message = state.error ?: "",
                onRetry = { viewModel.refresh() },
                modifier = Modifier.padding(padding),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.refresh(pullToRefresh = true) },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        Text(
                            "${state.total} reports from your meter (one per minute)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (state.items.isEmpty()) {
                        item { EmptyState("No readings yet. They appear once your meter reports.") }
                    }

                    items(state.items, key = { it.id }) { reading -> ReadingItem(reading) }

                    item {
                        ListFooter(
                            loading = state.loading,
                            canLoadMore = state.canLoadMore,
                            error = if (state.items.isNotEmpty()) state.error else null,
                            onLoadMore = viewModel::loadMore,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingItem(reading: Reading) {
    SectionCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDateTime(reading.createdAt),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                if (reading.totalCost > 0) "-${money(reading.totalCost)}" else money(0.0),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (reading.totalCost > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "⚡ ${number(reading.energyKwh, 4)} kWh   💧 ${number(reading.waterL, 2)} L",
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            "${number(reading.voltage, 1)} V · ${number(reading.current, 3)} A · ${number(reading.power, 1)} W · PF ${number(reading.pf, 2)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Balance after: ${money(reading.balanceAfter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
