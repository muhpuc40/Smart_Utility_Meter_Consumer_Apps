package com.smartmeter.consumer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter.consumer.data.local.Session
import com.smartmeter.consumer.data.remote.Profile
import com.smartmeter.consumer.ui.LocalAppContainer
import com.smartmeter.consumer.ui.components.ErrorBanner
import com.smartmeter.consumer.ui.components.ErrorState
import com.smartmeter.consumer.ui.components.InfoRow
import com.smartmeter.consumer.ui.components.LoadingState
import com.smartmeter.consumer.ui.components.ScreenScaffold
import com.smartmeter.consumer.ui.components.SectionCard
import com.smartmeter.consumer.ui.components.StatGrid
import com.smartmeter.consumer.ui.components.StatItem
import com.smartmeter.consumer.ui.components.StatusBadge
import com.smartmeter.consumer.ui.theme.EnergyColor
import com.smartmeter.consumer.ui.theme.SuccessColor
import com.smartmeter.consumer.ui.theme.WaterColor
import com.smartmeter.consumer.ui.util.money
import com.smartmeter.consumer.ui.util.number
import com.smartmeter.consumer.ui.util.relayColor
import com.smartmeter.consumer.ui.util.stateLabel
import com.smartmeter.consumer.ui.util.timeAgo
import kotlinx.coroutines.delay

private const val AUTO_REFRESH_MS = 30_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    session: Session,
    onRecharge: () -> Unit,
    onUsage: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: HomeViewModel = viewModel { HomeViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Auto-refresh while this screen is visible
    LaunchedEffect(Unit) {
        while (true) {
            delay(AUTO_REFRESH_MS)
            viewModel.load(silent = true)
        }
    }

    ScreenScaffold(title = "Hello, ${session.name.substringBefore(' ')}") { padding ->
        val profile = state.profile

        when {
            state.loading && profile == null -> LoadingState(
                modifier = Modifier.padding(padding),
                message = "Loading your meter...",
            )

            profile == null -> ErrorState(
                message = state.error ?: "Could not load data",
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.load(pullToRefresh = true) },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                HomeContent(
                    profile = profile,
                    error = state.error,
                    onRecharge = onRecharge,
                    onUsage = onUsage,
                )
            }
        }
    }
}

@Composable
private fun HomeContent(
    profile: Profile,
    error: String?,
    onRecharge: () -> Unit,
    onUsage: () -> Unit,
) {
    val meter = profile.meter

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (error != null) ErrorBanner("Showing last data. $error")

        BalanceCard(profile, onRecharge)

        SectionCard(title = "Live meter readings") {
            Text(
                if (meter.online) "Meter online · updated ${timeAgo(meter.lastSeenAt)}"
                else "Meter offline · last report ${timeAgo(meter.lastSeenAt)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            StatGrid(
                listOf(
                    StatItem("Voltage", "${number(meter.lastVoltage, 1)} V", Icons.Outlined.Bolt, EnergyColor),
                    StatItem("Current", "${number(meter.lastCurrent, 3)} A", Icons.Outlined.ElectricalServices, EnergyColor),
                    StatItem("Power", "${number(meter.lastPower, 1)} W", Icons.Outlined.Power, EnergyColor),
                    StatItem("Power factor", number(meter.lastPf, 2), Icons.Outlined.Tune, EnergyColor),
                    StatItem("Frequency", "${number(meter.lastFrequency, 1)} Hz", Icons.Outlined.GraphicEq, EnergyColor),
                    StatItem("Water flow", "${number(meter.lastFlowRate, 2)} L/min", Icons.Outlined.WaterDrop, WaterColor),
                ),
            )
        }

        SectionCard(title = "Total usage") {
            StatGrid(
                listOf(
                    StatItem("Electricity used", "${number(meter.totalEnergyKwh, 3)} kWh", Icons.Outlined.Bolt, EnergyColor),
                    StatItem("Water used", "${number(meter.totalWaterL, 1)} L", Icons.Outlined.WaterDrop, WaterColor),
                    StatItem("Electricity cost", money(meter.totalEnergyCost), Icons.Outlined.Payments, EnergyColor),
                    StatItem("Water cost", money(meter.totalWaterCost), Icons.Outlined.Payments, WaterColor),
                ),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onUsage, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("View daily usage")
            }
        }

        SectionCard(title = "Tariff") {
            InfoRow("Electricity", "${money(profile.tariff.electricityRate)} / kWh")
            HorizontalDivider()
            InfoRow("Water", "${money(profile.tariff.waterRate)} / 1000 L")
            HorizontalDivider()
            InfoRow("Supply turns off at", money(profile.tariff.minBalance))
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun BalanceCard(profile: Profile, onRecharge: () -> Unit) {
    val meter = profile.meter
    val primary = MaterialTheme.colorScheme.primary

    Surface(shape = RoundedCornerShape(24.dp), color = Color.Transparent) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(primary, primary.copy(alpha = 0.78f))),
                    RoundedCornerShape(24.dp),
                )
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Meter ${meter.meterId}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(if (meter.online) SuccessColor else Color.White.copy(alpha = 0.5f), CircleShape),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (meter.online) "Online" else "Offline",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Current balance", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            Text(
                money(meter.balance),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(Modifier.height(12.dp))
            Surface(shape = RoundedCornerShape(50), color = Color.White) {
                StatusBadge(
                    text = "● ${stateLabel(meter.state)}",
                    color = relayColor(meter.relay),
                )
            }

            Spacer(Modifier.height(18.dp))
            Button(
                onClick = onRecharge,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = primary),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Recharge", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
