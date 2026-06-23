package com.nomore.example.serviceexample.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.nomore.example.serviceexample.R

class CounterWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        Log.d(TAG, "onUpdate called")
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, 0)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_UPDATE_COUNTER -> {
                val counter = intent.getIntExtra(EXTRA_COUNTER, 0)
                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                    android.content.ComponentName(context, CounterWidget::class.java)
                )
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId, counter)
                }
            }
        }
    }

    companion object {
        private const val TAG = "CounterWidget"
        const val ACTION_UPDATE_COUNTER = "com.nomore.example.serviceexample.ACTION_UPDATE_COUNTER"
        const val EXTRA_COUNTER = "counter_value"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            counter: Int
        ) {
            Log.d(TAG, "updateAppWidget: counter=$counter")
            val views = RemoteViews(context.packageName, R.layout.widget_counter)
            views.setTextViewText(R.id.tv_counter_widget, counter.toString())
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
