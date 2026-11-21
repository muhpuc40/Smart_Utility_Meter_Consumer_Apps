package com.smartmeter.consumer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

data class Session(
    val token: String,
    val name: String,
    val meterId: String,
)

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val session: Session) : SessionState
}

// Keeps the login token on the device so the consumer stays logged in.
class SessionStore(context: Context) {

    private val dataStore = context.applicationContext.sessionDataStore

    // In-memory copy used by the HTTP interceptor
    @Volatile
    var token: String? = null
        private set

    val state: Flow<SessionState> = dataStore.data.map { prefs ->
        val savedToken = prefs[TOKEN]
        token = savedToken

        if (savedToken == null) {
            SessionState.LoggedOut
        } else {
            SessionState.LoggedIn(Session(savedToken, prefs[NAME].orEmpty(), prefs[METER_ID].orEmpty()))
        }
    }

    suspend fun save(token: String, name: String, meterId: String) {
        this.token = token
        dataStore.edit { prefs ->
            prefs[TOKEN] = token
            prefs[NAME] = name
            prefs[METER_ID] = meterId
        }
    }

    suspend fun updateName(name: String) {
        dataStore.edit { prefs -> prefs[NAME] = name }
    }

    suspend fun clear() {
        token = null
        dataStore.edit { prefs -> prefs.clear() }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val NAME = stringPreferencesKey("name")
        val METER_ID = stringPreferencesKey("meter_id")
    }
}
