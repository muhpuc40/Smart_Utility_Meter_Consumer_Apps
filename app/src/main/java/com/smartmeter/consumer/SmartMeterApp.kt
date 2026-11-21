package com.smartmeter.consumer

import android.app.Application
import android.content.Context
import com.smartmeter.consumer.data.MeterRepository
import com.smartmeter.consumer.data.local.SessionStore
import com.smartmeter.consumer.data.remote.ApiClient

// Simple manual dependency container (one instance for the whole app)
class AppContainer(context: Context) {
    private val session = SessionStore(context)
    val repository = MeterRepository(ApiClient(session).service, session)
}

class SmartMeterApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
