package com.rx.geminipro.workers

import android.content.Context
import android.util.Log
import androidx.work.*
import com.rx.geminipro.services.ActionLogger
import com.rx.geminipro.services.CommandExecutor
import java.util.concurrent.TimeUnit

class ActionQueueWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    companion object {
        private const val TAG = "ActionQueueWorker"
        private const val WORK_NAME = "action_queue"
        private const val KEY_COMMAND = "command_text"

        fun enqueueAction(context: Context, commandText: String, delaySeconds: Long = 0) {
            val data = Data.Builder()
                .putString(KEY_COMMAND, commandText)
                .build()

            val requestBuilder = OneTimeWorkRequestBuilder<ActionQueueWorker>()
                .setInputData(data)

            if (delaySeconds > 0) {
                requestBuilder.setInitialDelay(delaySeconds, TimeUnit.SECONDS)
            }

            WorkManager.getInstance(context)
                .enqueue(requestBuilder.build())

            Log.d(TAG, "Enqueued action: $commandText (delay: ${delaySeconds}s)")
        }

        fun cancelAll(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(WORK_NAME)
        }
    }

    override fun doWork(): Result {
        val commandText = inputData.getString(KEY_COMMAND) ?: return Result.failure()

        Log.d(TAG, "Executing queued action: $commandText")

        try {
            val executor = CommandExecutor(applicationContext)
            executor.processGeminiResponse(commandText)

            ActionLogger.getInstance()?.logAction(
                "queued_action_executed",
                commandText,
                "success"
            )

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Queued action failed: ${e.message}")
            ActionLogger.getInstance()?.logAction(
                "queued_action_executed",
                commandText,
                "failed: ${e.message}"
            )
            return Result.retry()
        }
    }
}
