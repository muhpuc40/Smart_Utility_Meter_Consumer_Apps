package com.smartmeter.consumer.data

import com.smartmeter.consumer.data.local.SessionState
import com.smartmeter.consumer.data.local.SessionStore
import com.smartmeter.consumer.data.remote.ApiError
import com.smartmeter.consumer.data.remote.ApiService
import com.smartmeter.consumer.data.remote.AppJson
import com.smartmeter.consumer.data.remote.BalanceInfo
import com.smartmeter.consumer.data.remote.ChangePasswordRequest
import com.smartmeter.consumer.data.remote.ConsumerInfo
import com.smartmeter.consumer.data.remote.LoginData
import com.smartmeter.consumer.data.remote.LoginRequest
import com.smartmeter.consumer.data.remote.PageMeta
import com.smartmeter.consumer.data.remote.Profile
import com.smartmeter.consumer.data.remote.Reading
import com.smartmeter.consumer.data.remote.Recharge
import com.smartmeter.consumer.data.remote.RechargeRequest
import com.smartmeter.consumer.data.remote.Transaction
import com.smartmeter.consumer.data.remote.UpdateProfileRequest
import com.smartmeter.consumer.data.remote.UsageSummary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.time.Instant

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>

    data class Error(
        val message: String,
        val fieldErrors: Map<String, String> = emptyMap(),
        val code: Int = 0,
    ) : ApiResult<Nothing>
}

data class Page<T>(
    val items: List<T>,
    val meta: PageMeta,
)

class MeterRepository(
    private val api: ApiService,
    private val session: SessionStore,
) {

    val sessionState: Flow<SessionState> = session.state

    // ------------------------------------------------------------
    // Auth
    // ------------------------------------------------------------

    suspend fun login(identifier: String, password: String): ApiResult<LoginData> {
        val result = request(logoutOnAuthError = false, call = {
            api.login(LoginRequest(identifier.trim(), password))
        }) { it.data }

        if (result is ApiResult.Success) {
            session.save(result.data.token, result.data.consumer.name, result.data.consumer.meterId)
        }

        return result
    }

    suspend fun logout() = session.clear()

    // ------------------------------------------------------------
    // Profile
    // ------------------------------------------------------------

    suspend fun profile(): ApiResult<Profile> =
        request(call = { api.profile() }) { it.data }

    suspend fun updateProfile(name: String, email: String, address: String): ApiResult<ConsumerInfo> {
        val result = request(call = {
            api.updateProfile(UpdateProfileRequest(name.trim(), email.trim(), address.trim()))
        }) { it.data }

        if (result is ApiResult.Success) session.updateName(result.data.name)

        return result
    }

    // A wrong current password returns 401, which must not log the user out
    suspend fun changePassword(current: String, new: String): ApiResult<String> =
        request(logoutOnAuthError = false, call = {
            api.changePassword(ChangePasswordRequest(current, new))
        }) { it.message ?: "Password changed" }

    // ------------------------------------------------------------
    // Balance + usage
    // ------------------------------------------------------------

    suspend fun balance(): ApiResult<BalanceInfo> =
        request(call = { api.balance() }) { it.data }

    suspend fun usageSummary(from: Instant, to: Instant): ApiResult<UsageSummary> =
        request(call = { api.usageSummary(from.toString(), to.toString()) }) { it.data }

    suspend fun readings(page: Int): ApiResult<Page<Reading>> =
        request(call = { api.readings(page, PAGE_SIZE) }) { Page(it.data.orEmpty(), it.meta ?: PageMeta()) }

    // ------------------------------------------------------------
    // Recharges + transactions
    // ------------------------------------------------------------

    suspend fun createRecharge(amount: Double, method: String, transactionRef: String, note: String?): ApiResult<Recharge> =
        request(call = {
            api.createRecharge(RechargeRequest(amount, method, transactionRef.trim(), note?.trim()?.ifBlank { null }))
        }) { it.data }

    suspend fun recharges(page: Int, status: String?): ApiResult<Page<Recharge>> =
        request(call = { api.recharges(page, PAGE_SIZE, status) }) { Page(it.data.orEmpty(), it.meta ?: PageMeta()) }

    suspend fun transactions(page: Int): ApiResult<Page<Transaction>> =
        request(call = { api.transactions(page, PAGE_SIZE) }) { Page(it.data.orEmpty(), it.meta ?: PageMeta()) }

    // ------------------------------------------------------------
    // Request helper: converts every outcome into ApiResult
    // ------------------------------------------------------------

    private suspend fun <B, T> request(
        logoutOnAuthError: Boolean = true,
        call: suspend () -> Response<B>,
        map: (B) -> T?,
    ): ApiResult<T> {
        return try {
            val response = call()
            val body = response.body()

            if (response.isSuccessful && body != null) {
                val value = map(body)
                if (value != null) ApiResult.Success(value) else ApiResult.Error("Unexpected server response")
            } else {
                // Expired token or suspended account: back to the login screen
                if (logoutOnAuthError && (response.code() == 401 || response.code() == 403)) {
                    session.clear()
                }
                parseError(response)
            }
        }
        catch (e: CancellationException) {
            throw e
        }
        catch (e: SocketTimeoutException) {
            ApiResult.Error("The server is starting up. Please try again in a moment.")
        }
        catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        }
        catch (e: Exception) {
            ApiResult.Error(e.message ?: "Something went wrong")
        }
    }

    private fun parseError(response: Response<*>): ApiResult.Error {
        val raw = runCatching { response.errorBody()?.string() }.getOrNull()
        val error = raw?.let { runCatching { AppJson.decodeFromString(ApiError.serializer(), it) }.getOrNull() }

        val message = error?.message ?: when (response.code()) {
            401 -> "Session expired. Please log in again."
            429 -> "Too many attempts. Please wait and try again."
            502, 503, 504 -> "The server is starting up. Please try again in a moment."
            in 500..599 -> "Server error. Please try again."
            else -> "Request failed (${response.code()})"
        }

        return ApiResult.Error(
            message = message,
            fieldErrors = error?.details.orEmpty().associate { it.field to it.message },
            code = response.code(),
        )
    }

    companion object {
        const val PAGE_SIZE = 20
    }
}
