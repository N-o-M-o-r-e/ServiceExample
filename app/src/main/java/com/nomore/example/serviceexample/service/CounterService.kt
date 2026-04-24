package com.nomore.example.serviceexample.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.nomore.example.serviceexample.MainActivity
import com.nomore.example.serviceexample.R
import com.nomore.example.serviceexample.receiver.StartCounterReceiver
import com.nomore.example.serviceexample.utils.CounterPreference
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES

class CounterService : LifecycleService() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundService()
        startCounter()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    private fun startCounter() {
        lifecycleScope.launch {
            while (isActive) {
                delay(3000L)
                val currentCount = CounterPreference.getCounter(this@CounterService)
                val newCount = currentCount + 1
                CounterPreference.saveCounter(this@CounterService, newCount)
                android.util.Log.d("CounterService", "Counter updated: $newCount")
            }
        }
    }

    private fun createNotificationChannel() {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ForegroundServiceType")
    private fun startForegroundService() {
        runCatching {
            val notification = createNotification()
            if (VERSION.SDK_INT >= VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Counter Service")
            .setContentText("Counter is running...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("CounterService", "Service destroyed")
    }

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "counter_channel"
        private const val CHANNEL_NAME = "Counter Service Channel"

        fun startService(context: Context) {
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, CounterService::class.java)
                )
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, CounterService::class.java))
        }
    }
}
