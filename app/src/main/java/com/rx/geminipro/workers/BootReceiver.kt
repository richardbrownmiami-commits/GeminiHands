package com.rx.geminipro.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.rx.geminipro.services.GeminiForegroundService

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed - restarting Gemini Hands services")

            // Restart foreground service
            GeminiForegroundService.start(context)

            // Re-schedule periodic workers
            NotificationCheckWorker.schedulePeriodic(context)

            Log.d(TAG, "Services restarted after boot")
        }
    }
}
