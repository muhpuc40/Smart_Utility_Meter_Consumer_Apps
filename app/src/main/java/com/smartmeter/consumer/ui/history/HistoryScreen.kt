package com.smartmeter.consumer.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter.consumer.data.remote.Recharge
import com.smartmeter.consumer.data.remote.Transaction
import com.smartmeter.consumer.ui.LocalAppContainer
import com.smartmeter.consumer.ui.components.EmptyState
import com.smartmeter.consumer.ui.components.ErrorState
import com.smartmeter.consumer.ui.components.ListFooter
import com.smartmeter.consumer.ui.components.LoadingState
import com.smartmeter.consumer.ui.components.ScreenScaffold
import com.smartmeter.consumer.ui.components.SectionCard
import com.smartmeter.consumer.ui.components.StatusBadge
import com.smartmeter.consumer.ui.theme.DangerColor
import com.smartmeter.consumer.ui.theme.SuccessColor
import com.smartmeter.consumer.ui.util.PagedState
import com.smartmeter.consumer.ui.util.capitalized
import com.smartmeter.consumer.ui.util.formatDateTime
import com.smartmeter.consumer.ui.util.methodLabel
import com.smartmeter.consumer.ui.util.money
import com.smartmeter.consumer.ui.util.rechargeStatusColor

@Composable
fun HistoryScreen() {
    val container = LocalAppContainer.current
    val viewModel: HistoryViewModel = viewModel { HistoryViewModel(container.repository) }

    val recharges by viewModel.recharges.collectAsStateWithLifecycle()
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val filter by viewModel.rechargeFilter.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    ScreenScaffold(title = "History") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Recharges") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Transactions") })
            }

            if (selectedTab == 0) {
                RechargeList(
                    state = recharges,
                    filter = filter,
                    onFilter = viewModel::setRechargeFilter,
                    onRefresh = viewModel::refreshRecharges,
                    onLoadMore = viewModel::loadMoreRecharges,
                )
            }
            else {
                TransactionList(
                    state = transactions,
                    onRefresh = viewModel::refreshTransactions,
                    onLoadMore = viewModel::loadMoreTransactions,
                )
            }
        }
    }
}

// ------------------------------------------------------------
// Recharges
// ------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RechargeList(
    state: PagedState<Recharge>,
    filter: String?,
    onFilter: (String?) -> Unit,
    onRefresh: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RECHARGE_FILTERS.forEach { status ->
                FilterChip(
                    selected = filter == status,
                    onClick = { onFilter(status) },
                    label = { Text(status?.let { capitalized(it) } ?: "All") },
                )
            }
        }

        PagedList(
            state = state,
            emptyText = "No recharges yet",
            onRefresh = onRefresh,
            onLoadMore = onLoadMore,
            itemKey = { it.id },
        ) { recharge -> RechargeItem(recharge) }
    }
}

@Composable
private fun RechargeItem(recharge: Recharge) {
    SectionCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(money(recharge.amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    listOfNotNull(methodLabel(recharge.method), recharge.transactionRef).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusBadge(capitalized(recharge.status), rechargeStatusColor(recharge.status))
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "Submitted ${formatDateTime(recharge.createdAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (recharge.processedAt != null) {
            Text(
                "${capitalized(recharge.status)} ${formatDateTime(recharge.processedAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (recharge.source == "admin") {
            Text(
                "Added by office",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        recharge.note?.let {
            Text("Note: $it", style = MaterialTheme.typography.bodySmall)
        }

        recharge.rejectReason?.let {
            Spacer(Modifier.height(4.dp))
            Text("Reason: $it", style = MaterialTheme.typography.bodySmall, color = DangerColor)
        }
    }
}

// ------------------------------------------------------------
// Transactions (balance ledger)
// ------------------------------------------------------------

@Composable
private fun TransactionList(
    state: PagedState<Transaction>,
    onRefresh: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
) {
    PagedList(
        state = state,
        emptyText = "No transactions yet",
        onRefresh = onRefresh,
        onLoadMore = onLoadMore,
        itemKey = { it.id },
    ) { transaction -> TransactionItem(transaction) }
}

@Composable
private fun TransactionItem(transaction: Transaction) {
    val credit = transaction.amount >= 0

    SectionCard(title = null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    if (transaction.type == "recharge") "Recharge" else "Balance adjustment",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatDateTime(transaction.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                (if (credit) "+" else "-") + money(kotlin.math.abs(transaction.amount)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (credit) SuccessColor else DangerColor,
            )
        }

        Spacer(Modifier.height(6.dp))
        Text(
            "Balance ${money(transaction.balanceBefore)} → ${money(transaction.balanceAfter)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        transaction.note?.takeIf { it.isNotBlank() }?.let {
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ------------------------------------------------------------
// Shared paginated list with pull-to-refresh
// ------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> PagedList(
    state: PagedState<T>,
    emptyText: String,
    onRefresh: (Boolean) -> Unit,
    onLoadMore: () -> Unit,
    itemKey: (T) -> Any,
    itemContent: @Composable (T) -> Unit,
) {
    when {
        !state.loaded && state.items.isEmpty() -> LoadingState()

        state.items.isEmpty() && state.error != null -> ErrorState(
            message = state.error,
            onRetry = { onRefresh(false) },
        )

        else -> PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { onRefresh(true) },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.items.isEmpty()) {
                    item { EmptyState(emptyText) }
                }

                items(state.items, key = itemKey) { item -> itemContent(item) }

                item {
                    ListFooter(
                        loading = state.loading,
                        canLoadMore = state.canLoadMore,
                        error = if (state.items.isNotEmpty()) state.error else null,
                        onLoadMore = onLoadMore,
                    )
                }
            }
        }
    }
}
