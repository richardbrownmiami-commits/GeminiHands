package com.rx.geminipro.workers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.rx.geminipro.MainActivity
import com.rx.geminipro.services.ActionLogger
import java.util.concurrent.TimeUnit

class GeminiWakeUpWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "GeminiWakeUpWorker"
        private const val WORK_NAME = "gemini_wakeup"
        private const val KEY_MESSAGE = "wakeup_message"

        fun scheduleOneTime(context: Context, delayMinutes: Long, message: String = "Hey Gemini, time to check in!") {
            val data = Data.Builder()
                .putString(KEY_MESSAGE, message)
                .build()

            val request = OneTimeWorkRequestBuilder<GeminiWakeUpWorker>()
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)

            Log.d(TAG, "Scheduled wake-up in $delayMinutes minutes")
        }

        fun schedulePeriodic(context: Context, intervalMinutes: Long = 60, message: String = "Periodic check-in") {
            val data = Data.Builder()
                .putString(KEY_MESSAGE, message)
                .build()

            val request = PeriodicWorkRequestBuilder<GeminiWakeUpWorker>(
                intervalMinutes, TimeUnit.MINUTES
            )
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "${WORK_NAME}_periodic",
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )

            Log.d(TAG, "Scheduled periodic wake-up every $intervalMinutes minutes")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            WorkManager.getInstance(context).cancelUniqueWork("${WORK_NAME}_periodic")
        }
    }

    override fun doWork(): Result {
        Log.d(TAG, "Wake-up worker triggered")

        val message = inputData.getString(KEY_MESSAGE) ?: "Hey Gemini!"

        try {
            // Launch the main activity with the wake-up message
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("wakeup_message", message)
                putExtra("auto_send", true)
            }
            applicationContext.startActivity(intent)

            ActionLogger.getInstance()?.logAction(
                "wakeup_triggered",
                message,
                "success"
            )

            Log.d(TAG, "Wake-up executed: $message")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Wake-up failed: ${e.message}")
            ActionLogger.getInstance()?.logAction(
                "wakeup_triggered",
                message,
                "failed: ${e.message}"
            )
            return Result.retry()
        }
    }
}
