package com.nomore.example.serviceexample.utils

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "counter_store")

object CounterDataStore {
    private val COUNTER_KEY = intPreferencesKey("counter_value")
    private val SERVICE_ENABLED_KEY = booleanPreferencesKey("service_enabled")

    fun getCounterFlow(context: Context): Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[COUNTER_KEY] ?: 0
    }

    suspend fun saveCounter(context: Context, value: Int) {
        context.dataStore.edit { prefs ->
            prefs[COUNTER_KEY] = value
        }
    }

    suspend fun getCounter(context: Context): Int {
        return context.dataStore.data.map { prefs ->
            prefs[COUNTER_KEY] ?: 0
        }.first()
    }

    suspend fun resetCounter(context: Context) {
        context.dataStore.edit { prefs ->
            prefs.remove(COUNTER_KEY)
        }
    }

    fun getServiceEnabledFlow(context: Context): Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[SERVICE_ENABLED_KEY] ?: false
    }

    suspend fun setServiceEnabled(context: Context, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SERVICE_ENABLED_KEY] = enabled
        }
    }

    suspend fun isServiceEnabled(context: Context): Boolean {
        return context.dataStore.data.map { prefs ->
            prefs[SERVICE_ENABLED_KEY] ?: false
        }.first()
    }
}
