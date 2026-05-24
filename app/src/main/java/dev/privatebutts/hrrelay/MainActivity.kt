package dev.privatebutts.hrrelay

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import androidx.wear.compose.material3.MaterialTheme
import dev.privatebutts.hrrelay.ui.MainScreen
import dev.privatebutts.hrrelay.ui.SettingsScreen

class MainActivity : ComponentActivity() {

    private val hasPermission = mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission.value = granted
    }

    private val hrPermission: String
        get() = if (Build.VERSION.SDK_INT >= 36) {
            "android.permission.health.READ_HEART_RATE"
        } else {
            Manifest.permission.BODY_SENSORS
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        handleIntent(intent)
        hasPermission.value = isHrPermissionGranted()

        if (!hasPermission.value) {
            Handler(Looper.getMainLooper()).post {
                permissionLauncher.launch(hrPermission)
            }
        }

        val settingsDataStore = SettingsDataStore(this)

        setContent {
            MaterialTheme {
                val perm by hasPermission
                val navController = rememberNavController()
                NavHost(navController, startDestination = "main") {
                    composable("main") {
                        MainScreen(
                            hasBodySensors = perm,
                            onRequestPermission = {
                                permissionLauncher.launch(hrPermission)
                            },
                            onStartService = {
                                HeartRateState.error.value = null
                                startForegroundService(
                                    Intent(this@MainActivity, HeartRateService::class.java)
                                )
                            },
                            onStopService = {
                                stopService(
                                    Intent(this@MainActivity, HeartRateService::class.java)
                                )
                            },
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            settingsDataStore = settingsDataStore,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        hasPermission.value = isHrPermissionGranted()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == "dev.privatebutts.hrrelay.SET_URL") {
            val url = intent.getStringExtra("url") ?: return
            Log.d("MainActivity", "Setting URL from intent: $url")
            val dataStore = SettingsDataStore(this)
            MainScope().launch {
                dataStore.saveServerUrl(url)
                Log.d("MainActivity", "URL saved")
            }
        }
    }

    private fun isHrPermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= 36) {
            ContextCompat.checkSelfPermission(
                this, "android.permission.health.READ_HEART_RATE"
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.BODY_SENSORS
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
}
