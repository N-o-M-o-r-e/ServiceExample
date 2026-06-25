package com.nomore.example.serviceexample.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nomore.example.serviceexample.service.CounterService
import com.nomore.example.serviceexample.utils.CounterDataStore

class ServiceWatchdogWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!CounterDataStore.isServiceEnabled(applicationContext)) {
            Log.d(TAG, "Service disabled — skipping")
            return Result.success()
        }

        if (!CounterService.isRunning) {
            Log.d(TAG, "Service not running — restarting")
            CounterService.startService(applicationContext)
        } else {
            Log.d(TAG, "Service already running")
        }

        return Result.success()
    }

    companion object {
        private const val TAG = "__WatchdogWorker"
        const val WORK_NAME = "service_watchdog"
    }
}
