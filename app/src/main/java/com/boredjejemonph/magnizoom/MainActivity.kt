package com.boredjejemonph.magnizoom

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.boredjejemonph.magnizoom.ui.theme.MagniZoomTheme

class MainActivity : ComponentActivity() {
    private lateinit var overlaySettingsLauncher: ActivityResultLauncher<Intent>
    private lateinit var notificationPermissionLauncher: ActivityResultLauncher<String>
    private var waitingForOverlayPermission = false
    private var overlayActive by mutableStateOf(false)
    private val overlayStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == MagnifierOverlayService.ACTION_STATE_CHANGED) {
                overlayActive = MagnifierOverlayService.isOverlayActive(this@MainActivity)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        overlaySettingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (waitingForOverlayPermission) {
                waitingForOverlayPermission = false
                if (canDrawOverlaysCompat()) {
                    startMagnifierOverlay()
                } else {
                    Toast.makeText(
                        this,
                        "Draw over other apps permission is required for the overlay.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
        notificationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            requestOverlayPermissionThenStart()
        }

        super.onCreate(savedInstanceState)
        syncOverlayState()
        ContextCompat.registerReceiver(
            this,
            overlayStateReceiver,
            IntentFilter(MagnifierOverlayService.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        enableEdgeToEdge()
        setContent {
            MagniZoomTheme(dynamicColor = false) {
                MagniZoomApp(
                    overlayActive = overlayActive,
                    onStartOverlay = ::requestNotificationThenOverlay
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        syncOverlayState()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(overlayStateReceiver) }
        super.onDestroy()
    }

    private fun requestNotificationThenOverlay() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }

        requestOverlayPermissionThenStart()
    }

    private fun requestOverlayPermissionThenStart() {
        if (canDrawOverlaysCompat()) {
            startMagnifierOverlay()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            waitingForOverlayPermission = true
            val packageSettings = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            try {
                overlaySettingsLauncher.launch(packageSettings)
            } catch (_: RuntimeException) {
                overlaySettingsLauncher.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            }
        } else {
            startMagnifierOverlay()
        }
    }

    private fun canDrawOverlaysCompat(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun startMagnifierOverlay() {
        val intent = Intent(this, MagnifierOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, intent)
        } else {
            startService(intent)
        }
        overlayActive = true
        Toast.makeText(this, "MagniZoom overlay started.", Toast.LENGTH_SHORT).show()
    }

    private fun syncOverlayState() {
        overlayActive = MagnifierOverlayService.isOverlayActive(this)
    }
}
