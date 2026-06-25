package com.nomore.example.serviceexample.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nomore.example.serviceexample.service.CounterService
import com.nomore.example.serviceexample.utils.CounterDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) return

        Log.d(TAG, "Boot completed")
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (CounterDataStore.isServiceEnabled(context)) {
                    Log.d(TAG, "Service enabled — starting")
                    CounterService.startService(context)
                } else {
                    Log.d(TAG, "Service disabled — skipping")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "__BootReceiver"
    }
}
