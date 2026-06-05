package com.rx.geminipro.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.rx.geminipro.services.ActionLogger
import com.rx.geminipro.services.GeminiNotificationListener
import java.util.concurrent.TimeUnit

class NotificationCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "NotifCheckWorker"
        private const val WORK_NAME = "notification_check"

        fun schedulePeriodic(context: Context, intervalMinutes: Long = 15) {
            val request = PeriodicWorkRequestBuilder<NotificationCheckWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )

            Log.d(TAG, "Scheduled notification check every $intervalMinutes minutes")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }

    override fun doWork(): Result {
        Log.d(TAG, "Notification check worker triggered")

        try {
            val listener = GeminiNotificationListener.instance
            if (listener != null) {
                val summary = listener.getNotificationSummary()
                val count = GeminiNotificationListener.notifications.size

                ActionLogger.getInstance()?.logAction(
                    "notification_check",
                    "Found $count notifications",
                    summary
                )

                Log.d(TAG, "Checked notifications: $count found")
            } else {
                Log.w(TAG, "Notification listener not active")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Notification check failed: ${e.message}")
            return Result.retry()
        }
    }
}
