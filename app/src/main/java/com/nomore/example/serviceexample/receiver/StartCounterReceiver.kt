package com.nomore.example.serviceexample.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nomore.example.serviceexample.service.CounterService

class StartCounterReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot completed - starting service")
        CounterService.startService(context)
    }

    companion object {
        private const val TAG = "StartCounterReceiver"
    }
}
