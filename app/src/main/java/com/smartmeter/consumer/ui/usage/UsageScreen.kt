package com.smartmeter.consumer.ui.usage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.smartmeter.consumer.data.remote.UsageDay
import com.smartmeter.consumer.data.remote.UsageSummary
import com.smartmeter.consumer.ui.LocalAppContainer
import com.smartmeter.consumer.ui.components.BarChart
import com.smartmeter.consumer.ui.components.EmptyState
import com.smartmeter.consumer.ui.components.ErrorBanner
import com.smartmeter.consumer.ui.components.ErrorState
import com.smartmeter.consumer.ui.components.LoadingState
import com.smartmeter.consumer.ui.components.ScreenScaffold
import com.smartmeter.consumer.ui.components.SectionCard
import com.smartmeter.consumer.ui.components.StatGrid
import com.smartmeter.consumer.ui.components.StatItem
import com.smartmeter.consumer.ui.theme.EnergyColor
import com.smartmeter.consumer.ui.theme.WaterColor
import com.smartmeter.consumer.ui.util.formatDay
import com.smartmeter.consumer.ui.util.money
import com.smartmeter.consumer.ui.util.number

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsageScreen(onOpenReadings: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: UsageViewModel = viewModel { UsageViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    ScreenScaffold(title = "Usage") { padding ->
        val summary = state.summary

        when {
            state.loading && summary == null -> LoadingState(Modifier.padding(padding))

            summary == null -> ErrorState(
                message = state.error ?: "Could not load usage",
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.load(pullToRefresh = true) },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                UsageContent(
                    state = state,
                    summary = summary,
                    onRange = viewModel::selectRange,
                    onMetric = viewModel::selectMetric,
                    onOpenReadings = onOpenReadings,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UsageContent(
    state: UsageUiState,
    summary: UsageSummary,
    onRange: (UsageRange) -> Unit,
    onMetric: (UsageMetric) -> Unit,
    onOpenReadings: () -> Unit,
) {
    val days = summary.daily

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UsageRange.entries.forEach { range ->
                    FilterChip(
                        selected = state.range == range,
                        onClick = { onRange(range) },
                        label = { Text(range.label) },
                    )
                }
            }
        }

        state.error?.let { error ->
            item { ErrorBanner(error) }
        }

        if (state.loading) {
            item { LoadingState(Modifier.height(120.dp)) }
        }

        item {
            SectionCard(title = "Last ${state.range.label}") {
                StatGrid(
                    listOf(
                        StatItem("Electricity", "${number(summary.totals.energyKwh, 3)} kWh", Icons.Outlined.Bolt, EnergyColor),
                        StatItem("Water", "${number(summary.totals.waterL, 1)} L", Icons.Outlined.WaterDrop, WaterColor),
                        StatItem("Electricity cost", money(summary.totals.energyCost), Icons.Outlined.Payments, EnergyColor),
                        StatItem("Water cost", money(summary.totals.waterCost), Icons.Outlined.Payments, WaterColor),
                    ),
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total cost", style = MaterialTheme.typography.titleMedium)
                    Text(
                        money(summary.totals.totalCost),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        item {
            SectionCard(title = "Daily ${state.metric.label.lowercase()}") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UsageMetric.entries.forEach { metric ->
                        FilterChip(
                            selected = state.metric == metric,
                            onClick = { onMetric(metric) },
                            label = { Text(metric.label) },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                if (days.isEmpty()) {
                    EmptyState("No usage in this period")
                }
                else {
                    BarChart(
                        values = days.map { metricValue(it, state.metric) },
                        color = metricColor(state.metric),
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDay(days.first().day), style = MaterialTheme.typography.labelSmall)
                        Text(formatDay(days.last().day), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        item {
            OutlinedButton(onClick = onOpenReadings, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("All meter readings")
            }
        }

        if (days.isNotEmpty()) {
            item {
                Text("Day by day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            items(days.reversed(), key = { it.day }) { day ->
                DayRow(day)
            }
        }
    }
}

@Composable
private fun DayRow(day: UsageDay) {
    SectionCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                formatDay(day.day),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(money(day.totalCost), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        HorizontalDivider()
        Spacer(Modifier.height(6.dp))
        Row {
            Text(
                "⚡ ${number(day.energyKwh, 3)} kWh · ${money(day.energyCost)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                "💧 ${number(day.waterL, 1)} L · ${money(day.waterCost)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun metricValue(day: UsageDay, metric: UsageMetric): Double = when (metric) {
    UsageMetric.COST -> day.totalCost
    UsageMetric.ENERGY -> day.energyKwh
    UsageMetric.WATER -> day.waterL
}

@Composable
private fun metricColor(metric: UsageMetric) = when (metric) {
    UsageMetric.COST -> MaterialTheme.colorScheme.primary
    UsageMetric.ENERGY -> EnergyColor
    UsageMetric.WATER -> WaterColor
}
