package com.nomore.example.serviceexample.utils

import android.content.Context
import androidx.core.content.edit

object CounterPreference {
    private const val PREF_NAME = "counter_pref"
    private const val KEY_COUNTER = "counter_value"

    fun saveCounter(context: Context, value: Int) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { putInt(KEY_COUNTER, value) }
    }

    fun getCounter(context: Context): Int {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getInt(KEY_COUNTER, 0)
    }

    fun resetCounter(context: Context) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        sharedPref.edit { remove(KEY_COUNTER) }
    }
}
