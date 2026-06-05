package com.rx.geminipro

import android.app.Application
import androidx.work.Configuration
import com.rx.geminipro.services.ActionLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GeminiProApplication : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()

        // Initialize Action Logger
        ActionLogger.init(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
