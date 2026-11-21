package com.smartmeter.consumer.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ------------------------------------------------------------
// Envelope: { "success": true, "data": ..., "meta": ... }
// ------------------------------------------------------------

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null,
    val meta: PageMeta? = null,
    val message: String? = null,
)

@Serializable
data class PageMeta(
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 0,
)

// { "success": false, "message": "...", "details": [{ "field": "...", "message": "..." }] }
@Serializable
data class ApiError(
    val success: Boolean = false,
    val message: String? = null,
    val details: List<FieldError> = emptyList(),
)

@Serializable
data class FieldError(
    val field: String = "",
    val message: String = "",
)

// ------------------------------------------------------------
// Auth
// ------------------------------------------------------------

@Serializable
data class LoginRequest(
    val identifier: String,
    val password: String,
)

@Serializable
data class LoginData(
    val token: String,
    val consumer: LoginConsumer,
)

@Serializable
data class LoginConsumer(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    @SerialName("meter_id") val meterId: String,
)

// ------------------------------------------------------------
// Profile
// ------------------------------------------------------------

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val status: String = "active",
    @SerialName("created_at") val createdAt: String? = null,
    val meter: Meter,
    val tariff: Tariff,
)

@Serializable
data class ConsumerInfo(
    val id: String,
    val name: String,
    val phone: String,
    val email: String? = null,
    val address: String? = null,
    val status: String = "active",
)

@Serializable
data class Meter(
    val id: String,
    @SerialName("meter_id") val meterId: String,
    val status: String = "active",
    val balance: Double = 0.0,
    @SerialName("total_energy_kwh") val totalEnergyKwh: Double = 0.0,
    @SerialName("total_water_l") val totalWaterL: Double = 0.0,
    @SerialName("total_energy_cost") val totalEnergyCost: Double = 0.0,
    @SerialName("total_water_cost") val totalWaterCost: Double = 0.0,
    @SerialName("last_voltage") val lastVoltage: Double? = null,
    @SerialName("last_current") val lastCurrent: Double? = null,
    @SerialName("last_power") val lastPower: Double? = null,
    @SerialName("last_frequency") val lastFrequency: Double? = null,
    @SerialName("last_pf") val lastPf: Double? = null,
    @SerialName("last_flow_rate") val lastFlowRate: Double? = null,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val online: Boolean = false,
    val relay: String = "off",
    val state: String = "",
)

@Serializable
data class Tariff(
    @SerialName("electricity_rate") val electricityRate: Double = 0.0,   // Tk per kWh
    @SerialName("water_rate") val waterRate: Double = 0.0,               // Tk per 1000 L
    @SerialName("min_balance") val minBalance: Double = 0.0,             // Tk
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class UpdateProfileRequest(
    val name: String,
    val email: String,
    val address: String,
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class MessageResponse(
    val success: Boolean = false,
    val message: String? = null,
)

// ------------------------------------------------------------
// Balance
// ------------------------------------------------------------

@Serializable
data class BalanceInfo(
    @SerialName("meter_id") val meterId: String = "",
    val relay: String = "off",
    val state: String = "",
    val balance: Double = 0.0,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    val online: Boolean = false,
)

// ------------------------------------------------------------
// Usage
// ------------------------------------------------------------

@Serializable
data class UsageSummary(
    val from: String = "",
    val to: String = "",
    val totals: UsageTotals = UsageTotals(),
    val daily: List<UsageDay> = emptyList(),
)

@Serializable
data class UsageTotals(
    @SerialName("energy_kwh") val energyKwh: Double = 0.0,
    @SerialName("water_l") val waterL: Double = 0.0,
    @SerialName("energy_cost") val energyCost: Double = 0.0,
    @SerialName("water_cost") val waterCost: Double = 0.0,
    @SerialName("total_cost") val totalCost: Double = 0.0,
)

@Serializable
data class UsageDay(
    val day: String,
    @SerialName("energy_kwh") val energyKwh: Double = 0.0,
    @SerialName("water_l") val waterL: Double = 0.0,
    @SerialName("energy_cost") val energyCost: Double = 0.0,
    @SerialName("water_cost") val waterCost: Double = 0.0,
    @SerialName("total_cost") val totalCost: Double = 0.0,
)

@Serializable
data class Reading(
    val id: Long,
    val voltage: Double? = null,
    val current: Double? = null,
    val power: Double? = null,
    val frequency: Double? = null,
    val pf: Double? = null,
    @SerialName("flow_rate") val flowRate: Double? = null,
    @SerialName("energy_kwh") val energyKwh: Double = 0.0,
    @SerialName("water_l") val waterL: Double = 0.0,
    @SerialName("energy_cost") val energyCost: Double = 0.0,
    @SerialName("water_cost") val waterCost: Double = 0.0,
    @SerialName("total_cost") val totalCost: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    @SerialName("created_at") val createdAt: String = "",
)

// ------------------------------------------------------------
// Recharges + transactions
// ------------------------------------------------------------

@Serializable
data class RechargeRequest(
    val amount: Double,
    val method: String,
    @SerialName("transaction_ref") val transactionRef: String,
    val note: String? = null,
)

@Serializable
data class Recharge(
    val id: String,
    val amount: Double = 0.0,
    val method: String = "",
    @SerialName("transaction_ref") val transactionRef: String? = null,
    val status: String = "pending",
    val source: String = "consumer",
    val note: String? = null,
    @SerialName("reject_reason") val rejectReason: String? = null,
    @SerialName("processed_at") val processedAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("balance_after") val balanceAfter: Double? = null,
)

@Serializable
data class Transaction(
    val id: Long,
    val type: String = "",
    val amount: Double = 0.0,
    @SerialName("balance_before") val balanceBefore: Double = 0.0,
    @SerialName("balance_after") val balanceAfter: Double = 0.0,
    @SerialName("recharge_id") val rechargeId: String? = null,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)
