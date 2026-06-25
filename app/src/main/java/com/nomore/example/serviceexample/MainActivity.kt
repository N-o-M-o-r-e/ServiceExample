package com.nomore.example.serviceexample

import android.annotation.SuppressLint
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.nomore.example.serviceexample.databinding.ActivityMainBinding
import com.nomore.example.serviceexample.service.CounterService
import com.nomore.example.serviceexample.utils.CounterDataStore
import com.nomore.example.serviceexample.worker.ServiceWatchdogWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.core.net.toUri

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "__MainActivity"
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshPermissionStates() }

    private val batterySettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { refreshPermissionStates() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initView()
        initServiceSwitch()
        initPermissionSwitches()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
        refreshServiceSwitch()
    }

    private fun initView() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun initServiceSwitch() {
        binding.switchService.setOnCheckedChangeListener { _, isChecked ->
            lifecycleScope.launch {
                CounterDataStore.setServiceEnabled(this@MainActivity, isChecked)
                if (isChecked) {
                    CounterService.startService(this@MainActivity)
                    enqueueWatchdog()
                } else {
                    CounterService.stopService(this@MainActivity)
                    cancelWatchdog()
                }
                updateStatusText(isChecked)
            }
        }
    }

    private fun refreshServiceSwitch() {
        lifecycleScope.launch {
            val enabled = CounterDataStore.isServiceEnabled(this@MainActivity)
            binding.switchService.setOnCheckedChangeListener(null)
            binding.switchService.isChecked = enabled
            updateStatusText(enabled)
            if (enabled && !CounterService.isRunning) {
                Log.d(TAG, "Service enabled but not running — restarting")
                CounterService.startService(this@MainActivity)
            }
            initServiceSwitch()
        }
    }

    private fun updateStatusText(enabled: Boolean) {
        binding.tvStatus.text = if (enabled) "Service: ON" else "Service: OFF"
    }

    private fun initPermissionSwitches() {
        binding.switchNotification.setOnClickListener {
            val granted = isNotificationPermissionGranted()
            Log.d(TAG, "Notification switch clicked, granted=$granted")
            if (!granted) {
                binding.switchNotification.isChecked = false
                requestNotificationPermission()
            }
        }

        binding.switchBatteryOptimization.setOnClickListener {
            val ignored = isBatteryOptimizationIgnored()
            Log.d(TAG, "Battery switch clicked, ignored=$ignored")
            if (!ignored) {
                binding.switchBatteryOptimization.isChecked = false
                openBatteryOptimizationSettings()
            }
        }
    }

    private fun refreshPermissionStates() {
        val notifGranted = isNotificationPermissionGranted()
        val batteryIgnored = isBatteryOptimizationIgnored()
        Log.d(TAG, "refreshPermissionStates: notifGranted=$notifGranted, batteryIgnored=$batteryIgnored")
        binding.switchNotification.isChecked = notifGranted
        binding.switchBatteryOptimization.isChecked = batteryIgnored

        binding.tvBatteryHint.text = if (isBatteryOptimizationIgnored()) {
            "Đã tắt tối ưu hóa pin ✓"
        } else {
            "Tắt tối ưu hóa pin để service hoạt động ổn định"
        }
    }

    private fun isNotificationPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    private fun openBatteryOptimizationSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:$packageName".toUri()
        }
        batterySettingsLauncher.launch(intent)
    }

    private fun enqueueWatchdog() {
        val request = PeriodicWorkRequestBuilder<ServiceWatchdogWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ServiceWatchdogWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun cancelWatchdog() {
        WorkManager.getInstance(this).cancelUniqueWork(ServiceWatchdogWorker.WORK_NAME)
    }
}
