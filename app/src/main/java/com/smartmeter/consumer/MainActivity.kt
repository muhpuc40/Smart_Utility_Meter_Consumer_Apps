package com.smartmeter.consumer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.smartmeter.consumer.ui.AppRoot
import com.smartmeter.consumer.ui.theme.SmartMeterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as SmartMeterApp).container

        setContent {
            SmartMeterTheme {
                AppRoot(container)
            }
        }
    }
}
