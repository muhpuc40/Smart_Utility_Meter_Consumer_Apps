package com.smartmeter.consumer.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.smartmeter.consumer.BuildConfig
import com.smartmeter.consumer.data.remote.Profile
import com.smartmeter.consumer.ui.LocalAppContainer
import com.smartmeter.consumer.ui.components.ErrorBanner
import com.smartmeter.consumer.ui.components.ErrorState
import com.smartmeter.consumer.ui.components.InfoRow
import com.smartmeter.consumer.ui.components.LoadingState
import com.smartmeter.consumer.ui.components.ScreenScaffold
import com.smartmeter.consumer.ui.components.SectionCard
import com.smartmeter.consumer.ui.components.StatusBadge
import com.smartmeter.consumer.ui.theme.DangerColor
import com.smartmeter.consumer.ui.theme.SuccessColor
import com.smartmeter.consumer.ui.util.capitalized
import com.smartmeter.consumer.ui.util.formatDate
import com.smartmeter.consumer.ui.util.money
import com.smartmeter.consumer.ui.util.relayColor
import com.smartmeter.consumer.ui.util.stateLabel
import com.smartmeter.consumer.ui.util.timeAgo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
) {
    val container = LocalAppContainer.current
    val viewModel: ProfileViewModel = viewModel { ProfileViewModel(container.repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    var confirmLogout by remember { mutableStateOf(false) }

    // Reload each time the screen is shown (e.g. after editing the profile)
    LaunchedEffect(Unit) { viewModel.load() }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?") },
            text = { Text("You will need your phone number or meter ID and password to log in again.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    viewModel.logout()
                }) { Text("Log out", color = DangerColor) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancel") } },
        )
    }

    ScreenScaffold(title = "Profile") { padding ->
        val profile = state.profile

        when {
            state.loading && profile == null -> LoadingState(Modifier.padding(padding))

            profile == null -> ErrorState(
                message = state.error ?: "Could not load profile",
                onRetry = { viewModel.load() },
                modifier = Modifier.padding(padding),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { viewModel.load(pullToRefresh = true) },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                ProfileContent(
                    profile = profile,
                    error = state.error,
                    onEditProfile = onEditProfile,
                    onChangePassword = onChangePassword,
                    onLogout = { confirmLogout = true },
                )
            }
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    error: String?,
    onEditProfile: () -> Unit,
    onChangePassword: () -> Unit,
    onLogout: () -> Unit,
) {
    val meter = profile.meter

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (error != null) ErrorBanner(error)

        // ---------- Header ----------
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    initials(profile.name),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(profile.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(profile.phone, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            StatusBadge(
                capitalized(profile.status),
                if (profile.status == "active") SuccessColor else DangerColor,
            )
        }

        // ---------- Account ----------
        SectionCard(title = "Account") {
            InfoRow("Phone", profile.phone)
            HorizontalDivider()
            InfoRow("Email", profile.email ?: "Not set")
            HorizontalDivider()
            InfoRow("Address", profile.address ?: "Not set")
            HorizontalDivider()
            InfoRow("Member since", formatDate(profile.createdAt))
        }

        // ---------- Meter ----------
        SectionCard(title = "Meter") {
            InfoRow("Meter ID", meter.meterId)
            HorizontalDivider()
            InfoRow("Meter status", capitalized(meter.status))
            HorizontalDivider()
            InfoRow("Supply", stateLabel(meter.state), valueColor = relayColor(meter.relay))
            HorizontalDivider()
            InfoRow("Balance", money(meter.balance))
            HorizontalDivider()
            InfoRow(
                "Connection",
                if (meter.online) "Online" else "Offline · ${timeAgo(meter.lastSeenAt)}",
                valueColor = if (meter.online) SuccessColor else DangerColor,
            )
        }

        // ---------- Tariff ----------
        SectionCard(title = "Tariff") {
            InfoRow("Electricity", "${money(profile.tariff.electricityRate)} / kWh")
            HorizontalDivider()
            InfoRow("Water", "${money(profile.tariff.waterRate)} / 1000 L")
        }

        // ---------- Actions ----------
        OutlinedButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Edit profile")
        }

        OutlinedButton(onClick = onChangePassword, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Change password")
        }

        OutlinedButton(
            onClick = onLogout,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Log out")
        }

        Text(
            "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(8.dp))
    }
}

private fun initials(name: String): String =
    name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }.ifEmpty { "?" }
