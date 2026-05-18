package com.boredjejemonph.magnizoom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionConfig
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class ScreenCapturePermissionActivity : ComponentActivity() {
    private var requestedMode = OverlayCaptureMode.TargetAppRealtime
    private val capturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultData = result.data
        if (result.resultCode == Activity.RESULT_OK && resultData != null) {
            val serviceIntent = Intent(this, MagnifierOverlayService::class.java).apply {
                action = MagnifierOverlayService.ACTION_CAPTURE_PERMISSION_RESULT
                putExtra(MagnifierOverlayService.EXTRA_CAPTURE_MODE, requestedMode.wireName)
                putExtra(MagnifierOverlayService.EXTRA_CAPTURE_RESULT_CODE, result.resultCode)
                putExtra(MagnifierOverlayService.EXTRA_CAPTURE_RESULT_DATA, resultData)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } else {
            val serviceIntent = Intent(this, MagnifierOverlayService::class.java).apply {
                action = MagnifierOverlayService.ACTION_CAPTURE_CANCELLED
            }
            startService(serviceIntent)
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedMode = OverlayCaptureMode.fromWireName(intent.getStringExtra(EXTRA_CAPTURE_MODE))
        if (savedInstanceState == null) {
            launchCapturePermission()
        }
    }

    private fun launchCapturePermission() {
        if (requestedMode == OverlayCaptureMode.TargetAppRealtime && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            requestedMode = OverlayCaptureMode.WholeScreenSnapshot
            Toast.makeText(
                this,
                "Target app live capture needs Android 14 or newer. Using a hidden-overlay screen snapshot instead.",
                Toast.LENGTH_LONG
            ).show()
        }

        val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val permissionIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val config = when (requestedMode) {
                OverlayCaptureMode.TargetAppRealtime -> MediaProjectionConfig.createConfigForUserChoice()
                OverlayCaptureMode.WholeScreenRealtime,
                OverlayCaptureMode.WholeScreenSnapshot -> MediaProjectionConfig.createConfigForDefaultDisplay()
            }
            projectionManager.createScreenCaptureIntent(config)
        } else {
            projectionManager.createScreenCaptureIntent()
        }

        if (requestedMode == OverlayCaptureMode.TargetAppRealtime) {
            Toast.makeText(this, "Choose one target app for live magnifier capture.", Toast.LENGTH_LONG).show()
        } else if (requestedMode == OverlayCaptureMode.WholeScreenSnapshot) {
            Toast.makeText(
                this,
                "Screen Shot captures once. The overlay hides briefly so it is not included.",
                Toast.LENGTH_LONG
            ).show()
        }
        capturePermissionLauncher.launch(permissionIntent)
    }

    companion object {
        private const val EXTRA_CAPTURE_MODE = "com.boredjejemonph.magnizoom.extra.CAPTURE_MODE"

        fun createIntent(context: Context, mode: OverlayCaptureMode): Intent {
            return Intent(context, ScreenCapturePermissionActivity::class.java)
                .putExtra(EXTRA_CAPTURE_MODE, mode.wireName)
        }
    }
}
