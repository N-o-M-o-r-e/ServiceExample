package com.nomore.example.serviceexample.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.nomore.example.serviceexample.MainActivity
import com.nomore.example.serviceexample.R
import com.nomore.example.serviceexample.utils.CounterDataStore
import com.nomore.example.serviceexample.widget.CounterWidget
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class CounterService : LifecycleService() {

    private var counter = 0

    private fun formatDuration(seconds: Int): String {
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (days > 0) {
            "${days}d ${hours}h ${minutes}m ${secs}s"
        } else if (hours > 0) {
            "${hours}h ${minutes}m ${secs}s"
        } else if (minutes > 0) {
            "${minutes}m ${secs}s"
        } else {
            "${secs}s"
        }
    }
    private val screenState = MutableStateFlow(true)

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> screenState.value = true
                Intent.ACTION_SCREEN_OFF -> screenState.value = false
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun setupIntervalFlow() {
        lifecycleScope.launch {
            screenState
                .flatMapLatest { screenOn ->
                    if (screenOn) intervalFlow(1000L) else emptyFlow()
                }
                .collect {
                    counter++
                    Log.d(TAG, "count=$counter (${formatDuration(counter)})")
                    updateNotification()
                    updateWidget()
                    if (counter % 10 == 0) {
                        CounterDataStore.saveCounter(this@CounterService, counter)
                    }
                }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand")
        return START_STICKY
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            priority = 1000
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Counter Service", NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this, NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(count: Int = counter): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("Counter Service")
            .setContentText(formatDuration(count))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun updateWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(this)
            val componentName = ComponentName(this, CounterWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                val views = RemoteViews(packageName, R.layout.widget_counter)
                views.setTextViewText(R.id.tv_counter_widget, formatDuration(counter))

                for (appWidgetId in appWidgetIds) {
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
                Log.d(TAG, "Widget updated, count=$counter")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating widget", e)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        Log.d(TAG, "onCreate")
        createNotificationChannel()
        startForegroundCompat()
        registerScreenReceiver()
        restoreCounter()
        setupIntervalFlow()
    }

    private fun restoreCounter() {
        lifecycleScope.launch {
            counter = CounterDataStore.getCounter(this@CounterService)
            Log.d(TAG, "Restored count=$counter")
            updateNotification()
            updateWidget()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        lifecycleScope.launch {
            CounterDataStore.saveCounter(this@CounterService, counter)
            Log.d(TAG, "Saved count=$counter")
        }
        Log.d(TAG, "onDestroy")
    }

    companion object {
        private const val TAG = "__CounterService"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "counter_channel"

        @Volatile
        var isRunning = false
            private set

        @SuppressLint("ForegroundServiceType")
        fun startService(context: Context) {
            Log.d(TAG, "startService called")
            ContextCompat.startForegroundService(
                context, Intent(context, CounterService::class.java)
            )
        }

        fun stopService(context: Context) {
            Log.d(TAG, "stopService called")
            context.stopService(Intent(context, CounterService::class.java))
        }
    }
}

fun intervalFlow(intervalMillis: Long): Flow<Unit> = channelFlow {
    while (isActive) {
        send(Unit)
        delay(intervalMillis)
    }
}
