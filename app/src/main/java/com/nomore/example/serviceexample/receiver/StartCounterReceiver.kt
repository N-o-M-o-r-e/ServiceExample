package com.nomore.example.serviceexample.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nomore.example.serviceexample.service.CounterService

class StartCounterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            android.util.Log.d("StartCounterReceiver", "Boot completed - starting service")
            CounterService.startService(context)
        }
    }
}
